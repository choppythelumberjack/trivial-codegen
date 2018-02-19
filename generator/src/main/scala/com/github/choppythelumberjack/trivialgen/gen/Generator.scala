package com.github.choppythelumberjack.trivialgen

import java.nio.charset.StandardCharsets
import java.nio.file._

import com.github.choppythelumberjack.trivialgen.GeneratorHelpers.indent
import com.github.choppythelumberjack.trivialgen.ScalaLangUtil.escape
import com.github.choppythelumberjack.trivialgen.ext.DatabaseTypes.DatabaseType
import com.github.choppythelumberjack.trivialgen.gen.{StereotypePackager, _}
import com.github.choppythelumberjack.trivialgen.model.StereotypingService.Namespacer
import com.github.choppythelumberjack.trivialgen.model._
import com.github.choppythelumberjack.trivialgen.util.DatabaseDiscoveryUtil
import com.github.choppythelumberjack.trivialgen.util.StringUtil._
import com.github.choppythelumberjack.trivialgen.util.StringSeqUtil._


trait SingleGeneratorFactory[+G] extends ((EmitterSettings) => G)

trait Generator extends  WithFileNaming {
  this: CodeGeneratorComponents with StereotypingService =>

  // Primarily to be used in child implementations in order to be able to reference SingleUnitCodegen
  // or some subclass of it that the child implementation will have. See how the ComposeableTraitsGen
  // uses this in the packaging strategy.
  override type Gen <: CodeEmitter

  def packagePrefix:String
  def configs: Seq[CodeGeneratorConfig]

  val databaseType: DatabaseType = DatabaseDiscoveryUtil.discoverDatabaseType(configs, connectionMaker(_))
  val schemaGetter: SchemaGetter = new DefaultSchemaGetter(databaseType)
  val namespacer: Namespacer = new DefaultNamespacer(schemaGetter)

  def generatorMaker = new SingleGeneratorFactory[CodeEmitter] {
    override def apply(emitterSettings: EmitterSettings): CodeEmitter =
      new CodeEmitter(emitterSettings)
  }

  class MultiGeneratorFactory[G](someGenMaker:SingleGeneratorFactory[G]) {
    def apply:Seq[G] = {
      val dataSources =
        configs.map(c => (c, connectionMaker(CodeGeneratorConfig(c.username, c.password, c.url))))

      val schemas =
        dataSources.map({ case (conf, ds) =>
          (conf, schemaReader(ds).filter(tbl => filter(tbl)))
        })

      // combine the generated elements as dictated by the packaging strategy and write the generator
      val configsAndGenerators =
        schemas.flatMap({ case (conf, schemas) =>
          val genProcess = new StereotypePackager[G]
          genProcess.packageIntoEmitters(someGenMaker, conf, packagingStrategy, stereotype(schemas))
        })

      configsAndGenerators
    }
  }
  def makeGenerators = new MultiGeneratorFactory[CodeEmitter](generatorMaker).apply

  def writeFiles(location: String) = {
    // can't put Seq[Gen] into here because doing Seq[Gen] <: SingleUnitCodegen makes it covariant
    // and args here needs to be contravariant
    def makeGenWithCorrespondingFile(gens:Seq[CodeEmitter]) = {
      type Method = (CodeEmitter) => Path

      gens.map(gen => {
        def DEFAULT_NAME = gen.defaultNamespace

        def tableName =
          gen.caseClassTables.headOption
            .orElse(gen.querySchemaTables.headOption)
            .map(_.table.name)
            .getOrElse(DEFAULT_NAME)

        val fileName: Path =
          (packagingStrategy.fileNamingStrategy, gen.codeWrapper) match {
            case (ByPackageObjectStandardName, _) =>
              Paths.get("package")

            // When the user wants to group tables by package, and use a standard package heading,
            // create a new package with the same name. For example say you have a
            // public.Person table (in schema.table notation) if a namespacer that
            // returns 'public' is used. The resulting file will be public/PublicExtensions.scala
            // which will have a 'Public' table definition
            case (ByPackageName, PackageHeader(packageName)) =>
              Paths.get(packageName, packageName)

            case (ByPackageName, _) =>
              Paths.get(gen.packageName.getOrElse(DEFAULT_NAME))

            case (ByTable, PackageHeader(packageName)) =>
              Paths.get(packageName, tableName)

            // First case classes table name or first Query Schemas table name, or default if both empty
            case (ByTable, _) =>
              Paths.get(gen.packageName.getOrElse(DEFAULT_NAME), tableName)

            case (ByDefaultName, _) =>
              Paths.get(DEFAULT_NAME)

            // Looks like 'Method' needs to be explicitly here since it doesn't understand Gen type annotation is actually SingleUnitCodegen
            case (BySomeTableData(method:Method), _) =>
              method(gen)
          }

        val fileWithExtension = fileName.resolveSibling(fileName.getFileName + ".scala")
        val loc = Paths.get(location)

        (gen, Paths.get(location, fileWithExtension.toString))
      })
    }

    val generatorsAndFiles = makeGenWithCorrespondingFile(makeGenerators)

    generatorsAndFiles.foreach({ case (gen, filePath) => {
        Files.createDirectories(filePath.getParent)
        val content = gen.apply
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))
      }
    })
  }

  val renderMembers = namingStrategy match {
    case CustomStrategy(_, _) => true
    case _ => false
  }

  /**
    * Run the Generator and return objects as strings
    *
    * @return
    */
  def writeStrings = makeGenerators.map(_.apply)


  class CodeEmitter(emitterSettings: EmitterSettings)
  extends AbstractCodeEmitter(emitterSettings) with PackageGen
  {
    import ScalaLangUtil._

    val config: CodeGeneratorConfig = emitterSettings.config
    val caseClassTables:Seq[TableStereotype] = emitterSettings.caseClassTables
    val querySchemaTables:Seq[TableStereotype] = if (namingStrategy.generateQuerySchemas) emitterSettings.querySchemaTables else Seq()
    override def codeWrapper:CodeWrapper = emitterSettings.codeWrapper

    /**
      * Use this when the for a particular schema is undefined but you need to have one
      * e.g. if you are writing to a file.
      */
    def defaultNamespace:String = "Schema"

    override def packagePrefix: String = Generator.this.packagePrefix

    override def code = surroundByPackage(body)
    def body: String = caseClassesCode + "\n\n" + tableSchemasCode

    def caseClassesCode:String = caseClassTables.map(CaseClass(_).code).mkString("\n\n")
    def tableSchemasCode:String = querySchemaTables.map(CombinedTableSchemas(_, memberNamer).code).mkString("\n")

    protected def ifMembers(str:String) = if (renderMembers) str else ""

    def CaseClass = new CaseClassGen(_)
    class CaseClassGen(val tableColumns:TableStereotype) extends super.AbstractCaseClassGen(tableColumns) with CaseClassNaming {
      def code = {
        s"case class ${actualCaseClassName}(" + tableColumns.columns.map(Member(_).code).mkString(", ") + ")"
      }

      def Member = new MemberGen(_)
      class MemberGen(val column:ColumnMash) extends super.AbstractMemberGen(column) with FieldNaming {
        override def rawType: String = column.dataType.toString()
        override def actualType: String = {
          val tpe = escape(rawType).replaceFirst("java\\.lang\\.", "")
          if (column.nullable) s"Option[${tpe}]" else tpe
        }
      }
    }

    def CombinedTableSchemas = new CombinedTableSchemasGen(_, _)
    class CombinedTableSchemasGen(
      tableColumns:TableStereotype,
      memberNamer: MemberNamer
    ) extends AbstractCombinedTableSchemasGen(tableColumns) with ObjectGen {

      override def code: String = surroundByObject(body)
      override def objectName: Option[String] = Some(escape(tableColumns.table.name))

      // TODO Have this come directly from the Generator's context (but make sure to override it in the structural tests so it doesn't distrub them)
      def imports = querySchemaImports

      // generate variables for every schema e.g.
      // foo = querySchema(...)
      // bar = querySchema(...)
      def body: String = {
        val schemas = tableColumns.table.meta.map(schema =>
          s"def ${memberNamer(schema)} = " + indent(QuerySchema(tableColumns, schema).code)
        ).mkString("\n\n")

        Seq(imports, schemas).pruneEmpty.mkString("\n\n")
      }

      def QuerySchema = new QuerySchemaGen(_, _)
      class QuerySchemaGen(val tableColumns:TableStereotype, schema:TableMeta) extends AbstractQuerySchemaGen(tableColumns, schema) with CaseClassNaming {

        def members =
          ifMembers(
            (tableColumns.columns.map(QuerySchemaMapping(_).code).mkString(",\n"))
          )

        override def code: String =
s"""
quote {
  ${indent(querySchema)}
}
""".stripMargin.trimFront

        def querySchema: String =
s"""
querySchema[${actualCaseClassName}](
  ${indent("\""+fullTableName+"\"")}${ifMembers(",")}
  ${indent(members)}
)
""".stripMargin.trimFront

        override def tableName: String = schema.tableName
        override def schemaName: String = schema.tableSchem

        def QuerySchemaMapping = new QuerySchemaMappingGen(_)
        class QuerySchemaMappingGen(val column:ColumnMash) extends AbstractQuerySchemaMappingGen(column) with FieldNaming {
          override def code: String = s"""_.${fieldName} -> "${databaseColumn}""""
          override def databaseColumn: String = column.meta.head.columnName
        }
      }
    }
  }
}

trait CaseClassNaming {
  def tableColumns:TableStereotype
  def rawCaseClassName:String = tableColumns.table.name
  def actualCaseClassName:String = escape(rawCaseClassName)
}

trait FieldNaming {
  def column:ColumnMash
  def rawFieldName:String = column.name
  def fieldName = escape(rawFieldName)
}

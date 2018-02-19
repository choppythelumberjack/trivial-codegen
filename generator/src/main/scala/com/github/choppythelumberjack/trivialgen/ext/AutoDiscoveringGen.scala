package com.github.choppythelumberjack.trivialgen.ext

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.github.choppythelumberjack.tryclose.JavaImplicits._
import com.github.choppythelumberjack.tryclose.{Failure, Success, TryClose}
import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.gen.CodeGeneratorConfig
import com.github.choppythelumberjack.trivialgen.util.DatabaseDiscoveryUtil
import com.github.choppythelumberjack.trivialgen.util.StringUtil._
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

/**
  * This generator builds on top of the <code>ComposeableTraitsGen</code> but does one other thing in addition:
  * it automatically generates a Quill database context. It does this by querying the databaseType, creating
  * the necessary context object, and then appends all the composeable context traits to this context.
  *
  * Say that you decide to connect to a H2 URL that looks like this:
  * <code>jdbc:h2:mem:sample...</code>. The <code>AutoDiscoveringGen</code> will query the Connection, and
  * discover that an H2 database is being used. If you specify that it should generate a mirror context, it will
  * then know that it is a <code>SqlMirrorContext</code> with an H2Dialect that needs to be generated.
  * Then it will proceed to generate pluggable traits for all the schemas.
  *
  * <pre>{@code
  * case class Person(firstName:String, lastName:String, age:Int)
  * case class Address(...)
  *
  * trait ContactsExtensions[Idiom <: io.getquill.idiom.Idiom, Naming <: io.getquill.NamingStrategy] {
  *   this:io.getquill.context.Context[Idiom, Naming] =>
  *
  *   object PersonDao { def query = querySchema(...) }
  *   object AddressDao { def query = querySchema(...) }
  * }
  *
  * trait OrdersExtensions[Idiom <: io.getquill.idiom.Idiom, Naming <: io.getquill.NamingStrategy] {
  *   this:io.getquill.context.Context[Idiom, Naming] =>
  *
  *   object OrderDao { def query = querySchema(...) }
  * }
  * }</pre>
  *
  * As the contacts, and orders traits are being generated, the AutoDiscoveringGen will keep track of them
  * and when the last file/code string needs to be generated, it will automatically create a H2-Compatible
  * mirror (or JDBC) context which will be appended with all the necessary schemas.
  *
  * <pre>
  * object MyCustomContext extends SqlMirrorContext[H2Dialect, Literal](H2Dialect, Literal)
  *   with ContactsExtensions[H2Dialect, Literal] with OrdersExtensions[H2Dialect, Literal]
  * </pre>
  *
  */
class AutoDiscoveringGen(
  override val configs: Seq[CodeGeneratorConfig],
  val contextType: ContextType = MirrorContext,
  override val packagePrefix: String = "",
  override val nestedTrait:Boolean = false,
  val contextName:String = "CombinedContext"
) extends ComposeableTraitsGen(configs, packagePrefix, nestedTrait) {

  def this(
    config: CodeGeneratorConfig,
    contextType: ContextType,
    packagePrefix: String,
    nestedTrait:Boolean
  ) = this(Seq(config), contextType, packagePrefix, nestedTrait)

  import DatabaseTypes._

  def quillIdiom:Class[_ <: io.getquill.NamingStrategy] = namingStrategy match {
    case LiteralNames => classOf[io.getquill.Literal]
    case SnakeCaseNames => classOf[io.getquill.SnakeCase]
    case SnakeCaseCustomTable(_) => classOf[io.getquill.SnakeCase]
    case CustomStrategy(_, _) => classOf[io.getquill.Literal]
  }
  def quillIdiomStr = quillIdiom.getName

  def quillContext = {
    val dialect = databaseType.dialect.getName

    def composeePath(gen:ContextifiedUnitGenerator) = gen.codeWrapper match {
      case NoWrapper => gen.traitName
      case PackageHeader(packageName) => delimited(packagePrefix, packageName, gen.traitName)(".")
      case PackageObject(packageName) => delimited(packagePrefix, packageName, gen.traitName)(".")
      case SimpleObject(packageName) => delimited(packagePrefix, packageName, gen.traitName)(".")
    }

    val namespaces =
      makeGenerators.map(
        gen => s"${composeePath(gen)}[${dialect}, ${quillIdiomStr}]").mkString("\n" + "  with ")

    val ret = contextType match {
      case MirrorContext => {
s"""
object ${contextName} extends io.getquill.SqlMirrorContext[${dialect}, ${quillIdiomStr}](${dialect}, ${quillIdiomStr})
  with ${namespaces}
""".stripMargin
      }

      case JdbcContext => {
        val jdbcContext = databaseType.context.getName
s"""
object ${contextName} extends ${jdbcContext}[${quillIdiomStr}](${quillIdiomStr})
   with ${namespaces}
""".stripMargin
      }
    }

    ret
  }

  def contextFileName = contextName + ".scala"

  override def writeFiles(location: String): Unit = {
    super.writeFiles(location)
    val context =
      s"package ${packagePrefix}\n"+
      quillContext

    Files.write(Paths.get(location, contextFileName), context.getBytes(StandardCharsets.UTF_8))
  }
}

sealed trait ContextType
case object MirrorContext extends ContextType
case object JdbcContext extends ContextType

object DatabaseTypes {
  import io.getquill._

  object DatabaseType {
    private def all:Seq[DatabaseType] = Seq(H2, MySql, SqlServer, Postgres, Sqlite)
    def fromProductName(productName:String) = {
      val res = all.find(_.databaseName == productName)
      if (res.isDefined) res.get
      else throw new IllegalArgumentException(
        s"Database type ${productName} not supported. Possible Values are: ${all.map(_.databaseName)}")
    }
  }

  sealed trait DatabaseType
    {def databaseName:String; def context:Class[_ <: JdbcContext[_, _]]; def dialect:Class[_ <: SqlIdiom]}
  case object H2 extends DatabaseType
    {def databaseName="H2"; def context=classOf[H2JdbcContext[_]]; def dialect = classOf[H2Dialect]}
  case object MySql extends DatabaseType
    {def databaseName="MySQL"; def context=classOf[MysqlJdbcContext[_]]; def dialect = classOf[MySQLDialect]}
  case object SqlServer extends DatabaseType
    {def databaseName="Microsoft SQL Server"; def context=classOf[SqlServerJdbcContext[_]]; def dialect = classOf[SQLServerDialect]}
  case object Postgres extends DatabaseType
    {def databaseName="PostgreSQL"; def context=classOf[PostgresJdbcContext[_]]; def dialect = classOf[PostgresDialect]}
  case object Sqlite extends DatabaseType
    {def databaseName="SQLite"; def context=classOf[SqliteJdbcContext[_]]; def dialect = classOf[SqliteDialect]}
}

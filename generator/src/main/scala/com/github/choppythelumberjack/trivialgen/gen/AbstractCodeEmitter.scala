package com.github.choppythelumberjack.trivialgen.gen

import com.github.choppythelumberjack.trivialgen.GeneratorHelpers.indent
import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.model.{ColumnMash, TableStereotype, TableMeta}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

case class EmitterSettings(
  config: CodeGeneratorConfig,
  caseClassTables:Seq[TableStereotype],
  querySchemaTables:Seq[TableStereotype],
  codeWrapper: CodeWrapper
)

abstract class AbstractCodeEmitter(emitterSettings: EmitterSettings) {
  def apply = code
  def code:String

  abstract class AbstractCaseClassGen(table:TableStereotype) {
    def code:String
    def rawCaseClassName:String
    def actualCaseClassName:String

    abstract class AbstractMemberGen(column:ColumnMash) {
      def code:String = s"${fieldName}: ${actualType}"
      def rawType:String
      def actualType:String
      def rawFieldName:String
      def fieldName:String
    }
  }

  abstract class AbstractCombinedTableSchemasGen(table:TableStereotype) {
    def code:String
    def imports:String

    abstract class AbstractQuerySchemaGen(table: TableStereotype, tableSchema: TableMeta) {
      def code: String
      def tableName: String
      def schemaName: String
      def fullTableName = s"${schemaName}.${tableName}"
      def rawCaseClassName: String
      def actualCaseClassName: String

      abstract class AbstractQuerySchemaMappingGen(column: ColumnMash) {
        def code: String
        def rawFieldName: String
        def fieldName: String
        def databaseColumn: String
      }

    }

  }
}

trait ObjectGen {
  def objectName:Option[String]
  def surroundByObject(innerCode:String) =
    objectName match {
      case None => innerCode
      case Some(objectNameActual) =>
s"""
object ${objectNameActual} {
  ${indent(innerCode)}
}
""".stripMargin.trimFront
    }
}

trait PackageGen {
  def packagePrefix:String

  def packageName:Option[String] = codeWrapper match {
    case NoWrapper => None
    case PackageHeader(packageName) => Some(packageName)
    case PackageObject(packageName) => Some(packageName)
    case SimpleObject(packageName) => Some(packageName)
  }

  def codeWrapper:CodeWrapper
  def surroundByPackage(innerCode:String) =
    codeWrapper match {
      case NoWrapper => innerCode
      case PackageHeader(packageName) => {
        val out = if (packagePrefix.trim != "") s"package ${packagePrefix}.${packageName}\n\n"
        else ""
        out + innerCode
      }

      case PackageObject(packageName) =>
s"""
package object ${packageName} {
  ${indent(innerCode)}
}
""".stripMargin.trimFront

      case SimpleObject(packageName) =>
s"""
object ${packageName} {
  ${indent(innerCode)}
}
""".stripMargin.trimFront
    }
}

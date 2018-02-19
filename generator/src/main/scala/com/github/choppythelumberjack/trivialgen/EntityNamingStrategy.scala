package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.model.{ColumnMeta, TableMeta}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

sealed trait EntityNamingStrategy { def generateQuerySchemas:Boolean = true }
sealed trait TrivialNamingStrategy extends EntityNamingStrategy { override def generateQuerySchemas:Boolean = false }

case object TrivialLiteralNames extends TrivialNamingStrategy
case object TrivialSnakeCaseNames extends TrivialNamingStrategy
case object LiteralNames extends EntityNamingStrategy
case object SnakeCaseNames extends EntityNamingStrategy
// TODO Need literal custom table
case class SnakeCaseCustomTable(
  tableParser: TableMeta => String
) extends EntityNamingStrategy
case class CustomStrategy(
  columnParser: ColumnMeta => String = cm=>cm.columnName.snakeToLowerCamel,
  tableParser: TableMeta => String = tm=>tm.tableName.snakeToUpperCamel
) extends EntityNamingStrategy

object EntityNamingStrategy {
  implicit class EntityNamingStrategyExtensions(strategy: EntityNamingStrategy) {
    def nameTable(tableSchema: TableMeta) = EntityNamingStrategy.nameTable(strategy, tableSchema)
    def nameColumn(columnSchema: ColumnMeta) = EntityNamingStrategy.nameColumn(strategy, columnSchema)
  }

  def nameTable(strategy: EntityNamingStrategy, tableSchema: TableMeta) = {
    strategy match {
      case TrivialLiteralNames => tableSchema.tableName.capitalize
      case TrivialSnakeCaseNames => tableSchema.tableName.snakeToUpperCamel
      case LiteralNames => tableSchema.tableName.capitalize
      case SnakeCaseNames => tableSchema.tableName.snakeToUpperCamel
      case SnakeCaseCustomTable(tableParser) => tableParser(tableSchema)
      case CustomStrategy(_, tableParser) => tableParser(tableSchema)
    }
  }
  def nameColumn(strategy: EntityNamingStrategy, columnSchema: ColumnMeta) = {
    strategy match {
      case TrivialLiteralNames => columnSchema.columnName
      case TrivialSnakeCaseNames => columnSchema.columnName.snakeToLowerCamel
      case LiteralNames => columnSchema.columnName
      case SnakeCaseNames => columnSchema.columnName.snakeToLowerCamel
      case SnakeCaseCustomTable(_) => columnSchema.columnName.snakeToLowerCamel
      case c:CustomStrategy => c.columnParser(columnSchema)
    }
  }
}

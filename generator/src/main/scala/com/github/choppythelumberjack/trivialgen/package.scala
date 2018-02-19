package com.github.choppythelumberjack

import java.sql.Connection

import com.github.choppythelumberjack.trivialgen.ext.DatabaseTypes.{DatabaseType, MySql, Postgres}
import com.github.choppythelumberjack.trivialgen.model._

import scala.reflect.ClassTag

package object trivialgen {

  case class JdbcTypeInfo(jdbcType:Int, typeName:String = "")
  object JdbcTypeInfo {
    def apply(cs: ColumnMeta):JdbcTypeInfo = JdbcTypeInfo(cs.dataType, cs.typeName)
  }

  type ConnectionMaker = () => Connection
  type JdbcTyper = JdbcTypeInfo => Option[ClassTag[_]]

  type SchemaReader = (ConnectionMaker) => Seq[TableSchema]
  type GeneratorEngine = (Seq[TableStereotype] => Seq[String])
  type MemberNamer = TableMeta => String
  type SchemaGetter = TableMeta => String

  class DefaultSchemaGetter(databaseType: DatabaseType) extends SchemaGetter {
    override def apply(meta: TableMeta): String = databaseType match {
      case MySql => meta.tableCat
      case _ => meta.tableSchem
    }
  }
}

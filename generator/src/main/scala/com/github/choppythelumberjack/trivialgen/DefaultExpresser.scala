package com.github.choppythelumberjack.trivialgen

import java.sql.DatabaseMetaData

import com.github.choppythelumberjack.trivialgen.model.StereotypingService.{Expresser, Namespacer}
import com.github.choppythelumberjack.trivialgen.model._
import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

class DefaultExpresser(
  entityNamingStrategy: EntityNamingStrategy,
  namespacer:Namespacer,
  typer:JdbcTyper
) extends Expresser {
  override def apply(schema: TableSchema): TableStereotype = {
    val tableModel = TableMash(
      namespacer(schema.table),
      entityNamingStrategy.nameTable(schema.table),
      Seq(schema.table)
    )
    val columnModels =
      schema.columns
        .map(desc => (desc, typer(JdbcTypeInfo(desc))))
        .filter { case (desc, tpe) => tpe.isDefined }
        .map { case (desc, tpe) =>
          ColumnMash(
            entityNamingStrategy.nameColumn(desc),
            tpe.get, // is safe to do this at this point since we filtered out nones
            desc.nullable != DatabaseMetaData.columnNoNulls,
            Seq(desc)
          )
        }

    TableStereotype(tableModel, columnModels)
  }
}

package com.github.choppythelumberjack.trivialgen.gen

import java.sql.{Connection, ResultSet}
import javax.sql.DataSource

import com.github.choppythelumberjack.tryclose._
import com.github.choppythelumberjack.trivialgen.{ConnectionMaker, SchemaReader}
import com.github.choppythelumberjack.trivialgen.model.{ColumnMeta, TableSchema, TableMeta}

import scala.annotation.tailrec
import scala.collection.immutable.List
import com.github.choppythelumberjack.tryclose.JavaImplicits._

class DefaultSchemaReader extends SchemaReader {

  @tailrec
  private def resultSetExtractor[T](rs:ResultSet, extractor:(ResultSet) => T, acc:List[T] = List()):List[T] = {
    if (!rs.next())
      acc.reverse
    else
      resultSetExtractor(rs, extractor, extractor(rs) :: acc)
  }

  private def extractTables(connectionMaker: () => Connection): List[TableMeta] = {
    val output = for {
      conn <- TryClose(connectionMaker())
      rs <- TryClose(conn.getMetaData.getTables(null, null, null, null))
      tables <- TryClose.wrap(resultSetExtractor(rs, rs => TableMeta.fromResultSet(rs)))
    } yield (tables)
    output.resolve.map(_.get) match {
      case Success(value) => value
      case Failure(e) => throw e
    }
  }

  private def extractColumns(connectionMaker: () => Connection): List[ColumnMeta] = {
    val output = for {
      conn <- TryClose(connectionMaker())
      rs <- TryClose(conn.getMetaData.getColumns(null, null, null, null))
      tables <- TryClose.wrap(resultSetExtractor(rs, rs => ColumnMeta.fromResultSet(rs)))
    } yield (tables)
    output.resolve.map(_.get) match {
      case Success(value) => value
      case Failure(e) => throw e
    }
  }


  override def apply(connectionMaker: ConnectionMaker): Seq[TableSchema] = {
    val tableMap =
      extractTables(connectionMaker)
      .map(t => ((t.tableCat, t.tableSchem, t.tableName), t))
      .toMap

    val columns = extractColumns(connectionMaker)
    val tableColumns =
      columns
        .groupBy(c => (c.tableCat, c.tableSchem, c.tableName))
        .map({case (tup, cols) => tableMap.get(tup).map(TableSchema(_, cols))})
        .collect({case Some(tbl) => tbl})

    tableColumns.toSeq
  }
}

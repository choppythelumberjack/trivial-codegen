package com.github.choppythelumberjack.trivialgen

import java.sql.ResultSet
import javax.sql.DataSource

import com.github.choppythelumberjack.tryclose.{Failure, Success, TryClose}

object MapMakingUtils {

  import com.github.choppythelumberjack.trivialgen.util.StringUtil._

  implicit class StringQueryExt(str:String) {
    def execute(dataSource: DataSource) = queryToMaps(str, dataSource)
  }

  def ccToMap(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {(a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }

  implicit class ToMapsExtensions(ccList:List[AnyRef]) {
    def toMaps = ccList.map(ccToMap(_))
    def toMapsUnpacked = {
      val maps = ccList.toMaps
      maps.map(_.map({
        case (k, Some(v)) => (k, v)
        case (k, other) => (k, other)
        case k:Any => throw new IllegalArgumentException(s"Invalid key ${k}")
      }))
    }
  }

  def resultsMap(rs:ResultSet): Map[String, Any] = {
    val meta = rs.getMetaData
    (1 to meta.getColumnCount)
      .map(meta.getColumnName(_))
      .map(colName => (colName.snakeToLowerCamel, rs.getObject(colName)))
      .toMap
  }

  def resultsToMaps(rs:ResultSet, acc:List[Map[String, Any]] = List()):List[Map[String, Any]] =
    if (!rs.next()) acc.reverse
    else resultsToMaps(rs, resultsMap(rs) :: acc)

  def queryToMaps(query:String, dataSource: DataSource):List[Map[String, Any]] = {
    import com.github.choppythelumberjack.tryclose.JavaImplicits._
    val resultMaps = for {
      ds <- TryClose.wrap(dataSource)
      conn <- TryClose(ds.get.getConnection)
      stmt <- TryClose(conn.createStatement)
      rs <- TryClose(stmt.executeQuery(query))
      maps <- TryClose.wrap(resultsToMaps(rs))
    } yield (maps)

    resultMaps.unwrap match {
      case Success(r) => r
      case Failure(e) => org.scalatest.Matchers.fail("Could not execute query produced by sql mirror context", e)
    }
  }
}

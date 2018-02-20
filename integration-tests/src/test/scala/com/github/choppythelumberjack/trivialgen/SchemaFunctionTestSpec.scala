package com.github.choppythelumberjack.trivialgen

import java.io.Closeable
import javax.sql.DataSource

import org.h2.jdbcx.JdbcDataSource
import org.scalatest.{FreeSpec, Matchers}

class SchemaFunctionTestSpec extends  FreeSpec with Matchers {

  implicit class OptExt[T](t:T) {
    def ? = Option[T](t)
  }

  def makeDataSource(schemaFile:String) = {
    val ds = new JdbcDataSource()
    // quoted tests seem to have issues unless IGNORECASE is enabled
    ds.setURL(s"jdbc:h2:mem:sample;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM '../generator/src/test/resources/${schemaFile}'")
    ds.setUser("sa")
    ds.setPassword("da")
    ds.asInstanceOf[DataSource with Closeable]
  }

  def snakecaseDS = makeDataSource("schema_snakecase.sql")
  def twoSchemaDS = makeDataSource("schema_snakecase_twoschema_differentcolumns_differenttypes.sql")
  def literalDS = makeDataSource("schema_casesensitive.sql")
}

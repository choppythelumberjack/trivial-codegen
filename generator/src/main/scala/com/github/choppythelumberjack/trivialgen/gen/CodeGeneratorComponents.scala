package com.github.choppythelumberjack.trivialgen.gen

import java.sql.DriverManager

import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.DefaultJdbcTyper
import com.github.choppythelumberjack.trivialgen.model.{TableSchema, TableStereotype}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._
import com.github.choppythelumberjack.trivialgen.{ConnectionMaker, JdbcTyper, MemberNamer, SchemaReader}

trait StandardCodeGeneratorComponents extends CodeGeneratorComponents {
  def packagePrefix:String

  def namingStrategy: EntityNamingStrategy = LiteralNames

  /**
    * When the code generator uses the Jdbc Typer to figure out which Scala/Java objects to use for
    * which JDBC type (e.g. use String for Varchar(...), Long for bigint etc...),
    * what do we do when we discover a JDBC type which we cannot translate (e.g. blob which is
    * currently not supported by quill). The simplest thing to do is to skip the column.
    */
  def unrecognizedTypeStrategy:UnrecognizedTypeStrategy = SkipColumn

  def typer:JdbcTyper = new DefaultJdbcTyper(unrecognizedTypeStrategy)
  def schemaReader: SchemaReader = new DefaultSchemaReader()
  def packagingStrategy: PackagingStrategy = PackagingStrategy.ByPackageHeader.TablePerFile(packagePrefix)

/**
  * When defining your query schema object, this will name the method which produces the query schema.
  * It will be named <code>query</code> by default so if you are doing Table Stereotyping, be sure
  * it's something reasonable like <code>(ts) => ts.tableName.snakeToLowerCamel</code>
  *
  * <pre>{@code
  * case class Person(firstName:String, lastName:String, age:Int)
  *
  * object Person {
  *   // The method will be 'query' by default which is good if you are not stereotyping.
  *   def query = querySchema[Person](...)
  * }
  * }</pre>
  *
  * Now let's take an example where you have a database that has two schemas <code>ALPHA</code> and <code>BRAVO</code>,
  * each with a table called Person and you want to stereotype the two schemas into one table case class.
  * In this case you have to be sure that memberNamer is something like <code>(ts) => ts.tableName.snakeToLowerCamel</code>
  * so you'll get a different method for every querySchema.
  *
  * <pre>{@code
  * case class Person(firstName:String, lastName:String, age:Int)
  *
  * object Person {
  *   // Taking ts.tableName.snakeToLowerCamel will ensure each one has a different name. Otherise
  *   // all of them will be 'query' which will result in a compile error.
  *   def alphaPerson = querySchema[Person]("ALPHA.PERSON", ...)
  *   def bravoPerson = querySchema[Person]("BRAVO.PERSON", ...)
  * }
  * }</pre>
  */
  def memberNamer: MemberNamer = (ts) => "query" //ts.tableName.snakeToLowerCamel

  /**
    * Method that creates the Connection object in some way. Since this can be done in multiple ways,
    * all I need is a method that does that in some way.
    */
  def connectionMaker(cs: CodeGeneratorConfig):ConnectionMaker =
    () => {DriverManager.getConnection(cs.url, cs.username, cs.password)}
}

trait CodeGeneratorComponents {

// Want to do this but can't for some reason.
//  type Gen <: AbstractPackageGen
//  def generator(
//    config:CodeGeneratorConfig,
//    caseClassTables:Seq[TableStereotype],
//    querySchemaTables:Seq[TableStereotype],
//    codeWrapper: CodeWrapper
//  ):Gen

  def defaultExcludedSchemas = Set("INFORMATION_SCHEMA")
  def querySchemaImports = ""
  def namingStrategy: EntityNamingStrategy
  def unrecognizedTypeStrategy:UnrecognizedTypeStrategy
  def typer:JdbcTyper
  def schemaReader: SchemaReader
  def packagingStrategy: PackagingStrategy
  def memberNamer:MemberNamer
  def connectionMaker(connectionSettings: CodeGeneratorConfig):ConnectionMaker
  def schemaGetter: SchemaGetter
  def filter(tc:TableSchema):Boolean =
    !defaultExcludedSchemas.map(_.toLowerCase).contains(schemaGetter(tc.table).toLowerCase)
}


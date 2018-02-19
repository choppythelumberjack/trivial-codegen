package com.github.choppythelumberjack.trivialgen.util

import com.github.choppythelumberjack.trivialgen.ext.DatabaseTypes.DatabaseType
import com.github.choppythelumberjack.trivialgen.gen.CodeGeneratorConfig
import com.github.choppythelumberjack.tryclose.{Failure, Success, TryClose}
import com.github.choppythelumberjack.trivialgen.ConnectionMaker
import com.github.choppythelumberjack.tryclose.JavaImplicits._

object DatabaseDiscoveryUtil {
  def discoverDatabaseType(
    configs: scala.Seq[CodeGeneratorConfig],
    connectionMaker: CodeGeneratorConfig => ConnectionMaker): DatabaseType =
  {
    val databaseTypes =
      configs
      .map(conf => {
        val productNameRequest = for {
          conn <- TryClose(connectionMaker(conf).apply)
          meta <- TryClose.wrap(conn.getMetaData)
          product <- TryClose.wrap(meta.get.getDatabaseProductName)
        } yield (product)

        productNameRequest.unwrap match {
          case Success(p) => p
          case Failure(e) => throw e
        }
      })
      .map(DatabaseType.fromProductName(_))

    // make sure it's distinct since only one database type is supported per generator
    databaseTypes.distinct.toList match {
      case head :: Nil => head
      case other => throw new IllegalArgumentException(
        s"Multiple database types are not supported for a single generator: ${other.mkString(", ")}")
    }
  }
}

package com.github.choppythelumberjack.trivialgen

import scala.reflect.ClassTag
import scala.reflect.classTag
import java.sql.Types._

import com.github.choppythelumberjack.trivialgen.JdbcTyper

sealed trait UnrecognizedTypeStrategy
case object AssumeString extends UnrecognizedTypeStrategy
case object SkipColumn extends UnrecognizedTypeStrategy
case object ThrowTypingError extends UnrecognizedTypeStrategy

class TypingError(private val message:String) extends RuntimeException(message) {
}

class DefaultJdbcTyper(strategy:UnrecognizedTypeStrategy) extends JdbcTyper {

  def apply(jdbcTypeInfo: JdbcTypeInfo): Option[ClassTag[_]] = {

    implicit def toSome[T](tag:ClassTag[_]) = Some(tag)

    // see TABLE B-1 of JSR-000221 JBDCTM API Specification 4.1 Maintenance Release
    // Mapping to corresponding Scala types where applicable
    jdbcTypeInfo.jdbcType match {
      case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR => classTag[String]
      case NUMERIC | DECIMAL => classTag[BigDecimal]
      case BIT | BOOLEAN => classTag[Boolean]
      case TINYINT => classTag[Byte]
      case SMALLINT => classTag[Int]
      case INTEGER => classTag[Int]
      case BIGINT => classTag[Long]
      case REAL => classTag[Float]
      case FLOAT | DOUBLE => classTag[Double]
      //case BINARY | VARBINARY | LONGVARBINARY | BLOB => classTag[java.sql.Blob]
      case DATE => classTag[java.time.LocalDate]
      case TIME => classTag[java.time.LocalDateTime]
      case TIMESTAMP => classTag[java.time.LocalDateTime]
      //case CLOB => classTag[java.sql.Clob]
      // case ARRAY => classTag[java.sql.Array]
      // case STRUCT => classTag[java.sql.Struct]
      // case REF => classTag[java.sql.Ref]
      // case DATALINK => classTag[java.net.URL]
      // case ROWID => classTag[java.sql.RowId]
      // case NCLOB => classTag[java.sql.NClob]
      // case SQLXML => classTag[java.sql.SQLXML]
      //case NULL => classTag[Null]
      //case DISTINCT => logger.warn(s"Found jdbc type DISTINCT. Assuming Blob. This may be wrong. You can override ModelBuilder#Table#Column#tpe to fix this."); classTag[java.sql.Blob] // FIXME
      //case t => println(s"Found unknown jdbc type $t. Assuming String. This may be wrong. You can override ModelBuilder#Table#Column#tpe to fix this."); classTag[String] // FIXME
      case _ => {
        strategy match {
          case AssumeString => classTag[String]
          case SkipColumn => None
          case ThrowTypingError => throw new TypingError(s"Could not resolve jdbc type: ${jdbcTypeInfo}")
        }
      }
    }
  }
}
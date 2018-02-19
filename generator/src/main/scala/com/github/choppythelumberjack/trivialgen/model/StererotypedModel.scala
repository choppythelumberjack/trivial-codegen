package com.github.choppythelumberjack.trivialgen.model

import com.github.choppythelumberjack.trivialgen.model._

import scala.reflect.ClassTag

case class TableStereotype(
  table: TableMash,
  columns: Seq[ColumnMash]
)
case class TableMash(
  namespace:String,
  name:String,
  meta:Seq[TableMeta]
)
case class ColumnMash(
  name:String,
  dataType:ClassTag[_],
  nullable:Boolean,
  meta:Seq[ColumnMeta]
)

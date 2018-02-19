package com.github.choppythelumberjack.trivialgen.model

import java.sql.ResultSet

import com.github.choppythelumberjack.trivialgen.{DefaultJdbcTyper, ScalaLangUtil}
import com.github.choppythelumberjack.trivialgen.model._

case class TableSchema(table: TableMeta, columns: Seq[ColumnMeta])

case class TableMeta(
  tableCat:String,
  tableSchem:String,
  tableName:String,
  tableType:String,
  remarks:String,
  typeCat:String,
  typeSchem:String,
  typeName:String,
  selfReferencingColName:String,
  refGeneration:String
)

object TableMeta {
  def fromResultSet(rs:ResultSet) = TableMeta(
    tableCat = rs.getString("TABLE_CAT"),
    tableSchem = rs.getString("TABLE_SCHEM"),
    tableName = rs.getString("TABLE_NAME"),
    tableType = rs.getString("TABLE_TYPE"),
    remarks = rs.getString("REMARKS"),
    typeCat = rs.getString("TYPE_CAT"),
    typeSchem = rs.getString("TYPE_SCHEM"),
    typeName = rs.getString("TYPE_NAME"),
    selfReferencingColName = rs.getString("SELF_REFERENCING_COL_NAME"),
    refGeneration = rs.getString("REF_GENERATION")
  )
}

case class ColumnMeta(
  tableCat:String,
  tableSchem:String,
  tableName:String,
  columnName:String,
  dataType:Int,
  typeName:String,
  columnSize:Int,
  decimalDigits:Int,
  numPrecRadix:Int,
  nullable:Int,
  remarks:String,
  columnDef:String,
  sqlDataType:Int,
  sqlDatetimeSub:Int,
  charOctetLength:Int,
  ordinalPosition:Int,
  isNullable:String,
  scopeCatalog:String,
  scopeSchema:String,
  scopeTable:String,
  sourceDataType:Int,
  isAutoincrement:String
  //isGeneratedcolumn:String
)

object ColumnMeta {
  def fromResultSet(rs:ResultSet) =
    ColumnMeta(
      tableCat = rs.getString("TABLE_CAT"),
      tableSchem = rs.getString("TABLE_SCHEM"),
      tableName = rs.getString("TABLE_NAME"),
      columnName = rs.getString("COLUMN_NAME"),
      dataType = rs.getInt("DATA_TYPE"),
      typeName = rs.getString("TYPE_NAME"),
      columnSize = rs.getInt("COLUMN_SIZE"),
      decimalDigits = rs.getInt("DECIMAL_DIGITS"),
      numPrecRadix = rs.getInt("NUM_PREC_RADIX"),
      nullable = rs.getInt("NULLABLE"),
      remarks = rs.getString("REMARKS"),
      columnDef = rs.getString("COLUMN_DEF"),
      sqlDataType = rs.getInt("SQL_DATA_TYPE"),
      sqlDatetimeSub = rs.getInt("SQL_DATETIME_SUB"),
      charOctetLength = rs.getInt("CHAR_OCTET_LENGTH"),
      ordinalPosition = rs.getInt("ORDINAL_POSITION"),
      isNullable = rs.getString("IS_NULLABLE"),
      scopeCatalog = rs.getString("SCOPE_CATALOG"),
      scopeSchema = rs.getString("SCOPE_SCHEMA"),
      scopeTable = rs.getString("SCOPE_TABLE"),
      sourceDataType = rs.getInt("SOURCE_DATA_TYPE"),
      isAutoincrement = rs.getString("IS_AUTOINCREMENT")
      //isGeneratedcolumn = rs.getString("IS_GENERATEDCOLUMN")
    )
}


object SchemaModel {

}

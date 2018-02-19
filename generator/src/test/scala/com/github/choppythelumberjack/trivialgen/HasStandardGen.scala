package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.gen.{CodeGeneratorConfig, StandardGenerator}
import com.github.choppythelumberjack.trivialgen.model.StereotypingService.Namespacer
import com.github.choppythelumberjack.trivialgen.model.TableSchema
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

trait HasStandardGen {

  def standardGen(
    schemaFile:String,
    tableFilter:TableSchema=>Boolean = _ => true,
    entityNamingStrategy: EntityNamingStrategy = LiteralNames,
    entityNamespacer: Namespacer = ts => ts.tableSchem,
    entityMemberNamer: MemberNamer = ts => ts.tableName.snakeToLowerCamel
  ) =
    new StandardGenerator(CodeGeneratorConfig(
      "sa", "sa", s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM '${schemaFile}'"
    )) {
      override def filter(tc: TableSchema): Boolean = super.filter(tc) && tableFilter(tc)
      override def namingStrategy: EntityNamingStrategy = entityNamingStrategy
      override val namespacer: Namespacer = entityNamespacer
      override def memberNamer: MemberNamer = entityMemberNamer
      override def packagingStrategy: PackagingStrategy = super.packagingStrategy
    }
}

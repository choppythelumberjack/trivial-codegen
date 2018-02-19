package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.gen.CodeGeneratorConfig
import org.scalatest.FreeSpec

class SchemaUsingSpec extends FreeSpec {

  def makeConfig(script: String) = CodeGeneratorConfig(
    "sa", "sa", s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM 'generator/src/test/resources/${script}'"
  )

  def pack(num:Int) = s"com.github.choppythelumberjack.trivialgen.generated.comp${num}"
  def path(num:Int) = s"integration-tests/src/test/scala/com/github/choppythelumberjack/trivialgen/generated/comp${num}"
  def twoSchemaConfig = makeConfig("schema_snakecase_twoschema_differentcolumns_differenttypes.sql")
  def snakecaseConfig = makeConfig("schema_snakecase.sql")
  def literalConfig = makeConfig("schema_casesensitive.sql")
}

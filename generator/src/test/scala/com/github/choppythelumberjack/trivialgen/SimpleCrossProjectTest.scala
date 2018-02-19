package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.ext.TrivialGen

class SimpleCrossProjectTest extends SchemaUsingSpec {

  override def pack(num:Int) = s"com.github.choppythelumberjack.trivialgen.generated.simp${num}"
  override def path(num:Int) = s"integration-tests/src/test/scala/com/github/choppythelumberjack/trivialgen/generated/simp${num}"

  "trivial codegen" - {
    "snakecase" in {
      val gen = new TrivialGen(snakecaseConfig, pack(0)) {
        override def namingStrategy = TrivialSnakeCaseNames
      }
      gen.writeFiles(path(0))
    }
    "literal" in {
      val gen = new TrivialGen(literalConfig, pack(1)) {
        //override def namingStrategy = TrivialLiteralNames // Should be default
      }
      gen.writeFiles(path(1))
    }
  }
}

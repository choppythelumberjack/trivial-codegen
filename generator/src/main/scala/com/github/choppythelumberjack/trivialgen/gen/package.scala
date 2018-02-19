package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.model.DefaultStereotypingService

package object gen {
  case class CodeGeneratorConfig(
    username:String,
    password:String,
    url:String
  )

  trait GeneratorBase extends
    Generator with DefaultStereotypingService { this: CodeGeneratorComponents =>
  }

  class StandardGenerator(val configs: Seq[CodeGeneratorConfig], val packagePrefix:String)
    extends GeneratorBase with StandardCodeGeneratorComponents {

    def this(config:CodeGeneratorConfig) = this(Seq(config), "")
  }
}

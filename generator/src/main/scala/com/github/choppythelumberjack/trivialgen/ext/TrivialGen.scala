package com.github.choppythelumberjack.trivialgen.ext

import com.github.choppythelumberjack.trivialgen.{TrivialLiteralNames, TrivialNamingStrategy}
import com.github.choppythelumberjack.trivialgen.gen.{CodeGeneratorConfig, StandardGenerator}

/**
  * The purpose of the trivial code generator is to generate simple case classes representing tables
  * in a database. Create one or multiple <code>CodeGeneratorConfig</code> objects
  * and call the <code>.writeFiles</code> or <code>.writeStrings</code> methods
  * on the code generator and the reset happens automatically.
  */
class TrivialGen(
  override val configs: Seq[CodeGeneratorConfig],
  override val packagePrefix: String = ""
) extends StandardGenerator(configs, packagePrefix) {

  def this(config: CodeGeneratorConfig, packagePrefix: String) = this(Seq(config), packagePrefix)

  override def namingStrategy: TrivialNamingStrategy = TrivialLiteralNames
}

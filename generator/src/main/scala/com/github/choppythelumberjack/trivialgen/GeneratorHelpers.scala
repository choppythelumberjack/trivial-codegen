package com.github.choppythelumberjack.trivialgen

object GeneratorHelpers {
  def indent(code: String): String = {
    val lines = code.split("\n")
    lines.tail.foldLeft(lines.head) { (out, line) =>
      out + '\n' +
        (if (line.isEmpty) line else "  " + line)
    }
  }
}

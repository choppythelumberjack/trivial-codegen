package com.github.choppythelumberjack.trivialgen.util

object StringSeqUtil {
  implicit class StringSeqExt(seq:Seq[String]) {
    def pruneEmpty = seq.filterNot(_.trim == "")
  }
}

package com.github.choppythelumberjack.trivialgen.util

import org.apache.commons.lang.StringUtils
import StringSeqUtil._

object StringUtil {
  def delimited(str:String*)(delimiter:String) = {
    str.toSeq.pruneEmpty.mkString(delimiter)
  }

  implicit class StringExtensions(str:String) {
    def snakeToUpperCamel = str.split("_").map(_.toLowerCase).map(_.capitalize).mkString
    def snakeToLowerCamel = str.split("_").map(_.toLowerCase).map(_.capitalize).mkString.uncapitalize
    def lowerCamelToSnake = str.split("(?=[A-Z])").mkString("_").toLowerCase
    def uncapitalize = StringUtils.uncapitalize(str)
    def unquote = str.replaceFirst("^\"", "").replaceFirst("\"$", "")
    def removeEmptyLines = str.replaceAll("(?m)^\\s", "")
    def trimFront = StringUtils.removeStart(str, "\n")
    def notEmpty = if (str.trim == "") None else Some(str)
  }
}

package com.github.choppythelumberjack.trivialgen

import scala.util.Try

object UnapplyTest {

  trait BaseTrait
  class ClassMember(val value:Int) extends BaseTrait
  object ClassMember {
    def unapply(arg: ClassMember): Option[Int] = Some(arg.value)
  }

  case class CaseClassMember(value:Int, foo:String) extends BaseTrait
  case class CaseClassMemberTwo(value:Long, bar:String) extends BaseTrait

  def unapplyExample() = {
    val v:BaseTrait = new ClassMember(1)
    v match {
      case CaseClassMember(i, str) => println("CaseClassMember")
      case CaseClassMemberTwo(i, str) => println("CaseClassMemberTwo")
      case ClassMember(i) => println("ClassMember")
      //case t => println(s"No Match value is: ${t}")
    }
  }

  def main(args:Array[String]) = {
    val v = for {
      foo <- Try("foo").filter(_.contains("fo"))
    } yield (foo)
    println(v)
  }
}

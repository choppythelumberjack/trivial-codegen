package com.github.choppythelumberjack.trivialgen

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{currentMirror => cm}

object ReflectUtil {

  def newCase[A]()(implicit t: ClassTag[A]): A = {
    val claas = cm classSymbol t.runtimeClass
    val modul = claas.companionSymbol.asModule
    val im = cm reflect (cm reflectModule modul).instance
    default[A](im, "apply")
  }


  private def default[A](im: InstanceMirror, name: String): A = {
    val at = newTermName(name)
    val ts = im.symbol.typeSignature
    val method = (ts member at).asMethod

    // either defarg or default val for type of p
    def valueFor(p: Symbol, i: Int): Any = {
      val defarg = ts member newTermName(s"$name$$default$$${i+1}")
      if (defarg != NoSymbol) {
        println(s"default $defarg")
        (im reflectMethod defarg.asMethod)()
      } else {
        println(s"def val for $p")
        p.typeSignature match {
          case t if t =:= typeOf[String] => null
          case t if t =:= typeOf[Int]    => 0
          case t if t =:= typeOf[Long]    => 0L
          case t if t =:= typeOf[Double]    => 0D
          case t if t =:= typeOf[Float]     => 0F
          case t if t =:= typeOf[java.util.Date]     => new java.util.Date
          case t if t =:= typeOf[java.time.LocalDate]     => java.time.LocalDate.now()
          case t if t =:= typeOf[java.time.LocalDateTime]     => java.time.LocalDateTime.now()
          case x                        => throw new IllegalArgumentException(x.toString)
        }
      }
    }
    val args = (for (ps <- method.paramss; p <- ps) yield p).zipWithIndex map (p => valueFor(p._1,p._2))
    (im reflectMethod method)(args: _*).asInstanceOf[A]
  }
}

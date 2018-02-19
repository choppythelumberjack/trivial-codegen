package com.github.choppythelumberjack.trivialgen.dag

import java.sql.Clob

import com.github.choppythelumberjack.trivialgen.MapExtensions
import com.github.choppythelumberjack.trivialgen.dag.dag.ClassAncestry

import scala.reflect.ClassTag
import scala.reflect.classTag


class DagNode(val cls:ClassTag[_], val parent:Option[DagNode])

trait NodeCatalog {
  def lookup(cls:ClassTag[_]):DagNode
}
object DefaultNodeCatalog extends NodeCatalog {

  implicit def nodeToOpt(dagNode: DagNode) = Some(dagNode)

  object StringNode extends DagNode(classTag[String], None)

  object BigDecimalNode extends DagNode(classTag[BigDecimal], StringNode)
  object DoubleNode extends DagNode(classTag[Double], BigDecimalNode)
  object FloatNode extends DagNode(classTag[Float], DoubleNode)

  object LongNode extends DagNode(classTag[Long], BigDecimalNode)
  object IntNode extends DagNode(classTag[Int], LongNode)
  object ByteNode extends DagNode(classTag[Byte], IntNode)
  object BooleanNode extends DagNode(classTag[Boolean], IntNode)

  object TimestampNode extends DagNode(classTag[java.time.LocalDateTime], StringNode)
  object DateNode extends DagNode(classTag[java.time.LocalDate], TimestampNode)

  protected[trivialgen] val nodeCatalogNodes:Seq[DagNode] = Seq(
    StringNode,
    BigDecimalNode,
    DoubleNode,
    FloatNode,
    LongNode,
    IntNode,
    ByteNode,
    BooleanNode,
    TimestampNode,
    DateNode
  )

  override def lookup(cls: ClassTag[_]): DagNode = nodeCatalogNodes.find(_.cls == cls).getOrElse({
    println(s"Could not find type hiearchy node for: ${} Must assume it's a string")
    StringNode
  })
}

package object dag {
  type ClassAncestry = (ClassTag[_], ClassTag[_]) => ClassTag[_]
}


class CatalogBasedAncestry(ancestryCatalog:NodeCatalog = DefaultNodeCatalog) extends ClassAncestry {

  def apply(one: ClassTag[_], two: ClassTag[_]): ClassTag[_] = {
    import scala.collection.immutable.::

    def getAncestry(node:DagNode): List[DagNode] = node.parent match {
      case Some(parent) => node :: getAncestry(parent)
      case None => node :: Nil
    }

    val oneAncestry = getAncestry(ancestryCatalog.lookup(one))
    val twoAncestry = getAncestry(ancestryCatalog.lookup(two))

    val (node, _) = MapExtensions.zipMaps(
      oneAncestry.zipWithIndex.toMap,
      twoAncestry.zipWithIndex.toMap)
      .collect {case (key, (Some(i), Some(j))) => (key, i+j)}
      .toList
      .sortBy {case(node, order) => order}
      .head

    node.cls
  }
}

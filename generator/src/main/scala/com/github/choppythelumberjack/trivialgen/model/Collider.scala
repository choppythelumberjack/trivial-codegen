package com.github.choppythelumberjack.trivialgen.model

import com.github.choppythelumberjack.trivialgen.dag.CatalogBasedAncestry
import com.github.choppythelumberjack.trivialgen.dag.dag.ClassAncestry
import com.github.choppythelumberjack.trivialgen.MapExtensions

import scala.collection.immutable.ListMap
import StereotypingService.Collider

// TODO Rename to Fuser
trait ColliderBase extends Collider {

  val ancestry:ClassAncestry

  protected def unifyColumns(a:ColumnMash, b:ColumnMash) = {
    val commonAncestor = ancestry(a.dataType, b.dataType)
    ColumnMash(
      a.name,
      commonAncestor,
      a.nullable || b.nullable,
      a.meta ++ b.meta
    )
  }

  protected def collideColumnSets(a:Seq[ColumnMash], b:Seq[ColumnMash]): Seq[ColumnMash] = {
    // join the two sets of columns by name, take only the ones with columns in common
    // and then unify the columns.
    MapExtensions.zipMapsOrdered[String, ColumnMash](
      new ListMap() ++ a.map(c => (c.name, c)),
      new ListMap() ++ b.map(c => (c.name, c))
    )
    .map({case (_, column) => column})
    .collect {
      case (Some(a), Some(b)) => unifyColumns(a, b)
    }.toSeq
  }

  protected def collideColumns(columnTables:Seq[TableStereotype]):Seq[ColumnMash] = {
    columnTables
      .map( _.columns)
      //.reduce {case (a,b) => a ++ b}
      .reduce((a, b) => collideColumnSets(a,b))
  }
}

class DefaultCollider(val ancestry: ClassAncestry = new CatalogBasedAncestry()) extends ColliderBase
{
  override def apply(collidingTables: Seq[TableStereotype]): TableStereotype = {
    TableStereotype(
      // Grab all the table schemas from the tables merged since we might want to keep track of them later
      collidingTables.head.table.copy(
        meta = collidingTables.flatMap(_.table.meta)
      ),
      collideColumns(collidingTables)
    )
  }
}

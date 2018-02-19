package com.github.choppythelumberjack.trivialgen.model

import com.github.choppythelumberjack.trivialgen.gen.DefaultSchemaReader
import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.SchemaGetter
import com.github.choppythelumberjack.trivialgen.model.StereotypingService.Namespacer
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

object StereotypingService {
  type Namespacer = TableMeta => String
  type Expresser = (TableSchema) => TableStereotype
  type Collider = (Seq[TableStereotype]) => TableStereotype
}

class DefaultNamespacer(schemaGetter: SchemaGetter) extends Namespacer {
  override def apply(ts: TableMeta): String = schemaGetter(ts).snakeToLowerCamel
}

trait StereotypingService {
  def stereotype(schemas:Seq[TableSchema]): Seq[TableStereotype]
}

trait DefaultStereotypingService extends StereotypingService {
  import StereotypingService._

  def namespacer: Namespacer

  def namingStrategy: EntityNamingStrategy
  def stereotype(schemas:Seq[TableSchema]): Seq[TableStereotype] = new DefaultStereotyper().apply(schemas)
  def collider: Collider = new DefaultCollider
  def expresser: Expresser = new DefaultExpresser(namingStrategy, namespacer, new DefaultJdbcTyper(AssumeString))

  type Stereotyper = (Seq[TableSchema]) => Seq[TableStereotype]
  class DefaultStereotyper extends Stereotyper {
    override def apply(schemaTables: Seq[TableSchema]): Seq[TableStereotype] = {

      // convert description objects into expression objects
      val expressionTables = schemaTables.map(expresser(_))

      // group by the namespaces
      val groupedExpressions =
        expressionTables.groupBy(tc => (tc.table.namespace, tc.table.name))

      // unify expression objects
      val unified = groupedExpressions.map({ case (_, seq) => collider(seq) })

      unified.toSeq
    }
  }

}


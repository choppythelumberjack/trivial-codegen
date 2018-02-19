package com.github.choppythelumberjack.trivialgen.gen

import com.github.choppythelumberjack.trivialgen.MapExtensions.zipMaps
import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.model.TableStereotype
import com.github.choppythelumberjack.trivialgen.CodeWrapper

/**
  * Packages stereotyped tables into emitters.
  */
class StereotypePackager[+G] {

  def packageIntoEmitters[H >: G](
    emitterMaker:(EmitterSettings) => H,
    conf: CodeGeneratorConfig,
    packagingStrategy: PackagingStrategy,
    tables: Seq[TableStereotype]) =
  {
    import MapExtensions._
    import packagingStrategy._
    implicit class OptionSeqExtentions[T](optionalSeq:Option[Seq[T]]) {
      def toSeq = optionalSeq match {
        case Some(seq) => seq
        case None => Seq[T]()
      }
    }

    // Generate (Packaging, [TableA, TableB, TableC]) objects for case classes
    val caseClassDefs = groupByNamespace(tables, packageNamingStrategyForCaseClasses)
    // Generate (Packaging, [TableA, TableB, TableC]) objects for query schemas
    val querySchemaDefs = groupByNamespace(tables, packageNamingStrategyForQuerySchemas)
    // Merge the two
    val mergedDefinitions =
      zipMaps(caseClassDefs, querySchemaDefs)
        .map({case (wrap, (caseClassOpt, querySchemaOpt)) => (wrap, (caseClassOpt.toSeq, querySchemaOpt.toSeq))})
        .map({case (wrap, (caseClassSeq, querySchemaSeq)) =>
          makeCodegenForNamespace(
            emitterMaker,
            conf, packageGroupingStrategy, wrap, caseClassSeq, querySchemaSeq)
        })

    mergedDefinitions.toSeq.flatMap(s => s)
  }

  protected def makeCodegenForNamespace[H >: G](
    generatorMaker:(EmitterSettings) => H,
    conf:CodeGeneratorConfig,
    groupingStrategy: PackageGroupingStrategy,
    wrapping: CodeWrapper,
    caseClassTables: Seq[TableStereotype],
    querySchemaTables: Seq[TableStereotype]
  ): Seq[H] = {

    groupingStrategy match {
      // if we are not supposed to group, create a unique codegen for every
      // [TableA, TableB, TableC] => { case class TableA(...); object TableA {querySchemaA, querySchemaB, etc...} } { case class TableB... }
      case DoNotGroup => {
        (caseClassTables ++ querySchemaTables)
          .groupBy(tc => tc)
          .map(_._1)
          .map(tc => generatorMaker(EmitterSettings(conf, Seq(tc), Seq(tc), wrapping)))
          .toSeq
      }
      case GroupByPackage =>
        Seq(generatorMaker(EmitterSettings(conf, caseClassTables, querySchemaTables, wrapping)))
    }
  }

  def groupByNamespace(
    tables: Seq[TableStereotype],
    packageNamingStrategy: PackageNamingStrategy
  ): Map[CodeWrapper, Seq[TableStereotype]] = {
    val mapped = tables.map(tbl => (packageNamingStrategy.apply(tbl), tbl))
    mapped
      .groupBy({ case (pack, _) => pack })
      .map({ case (pack, seq) => (pack, seq.map(_._2)) })
  }

}

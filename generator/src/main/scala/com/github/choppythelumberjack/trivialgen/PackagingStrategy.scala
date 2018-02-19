package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.gen.AbstractCodeEmitter
import com.github.choppythelumberjack.trivialgen.model.TableStereotype


object PackagingStrategy {

  object ByPackageObject {
    private def packageByNamespace(prefix:String) = PackageObjectByNamespace(prefix, _.table.namespace)

    def Simple(packagePrefix:String = "") =
      PackagingStrategy(
        GroupByPackage,
        packageByNamespace(packagePrefix),
        packageByNamespace(packagePrefix),
        ByPackageObjectStandardName)

    def CommonCaseClassOriginalSchemas(packagePrefix:String = "") =
      PackagingStrategy(
        GroupByPackage,
        packageByNamespace(packagePrefix),
        PackageObjectByNamespace(packagePrefix, _.table.meta.head.tableSchem),
        ByPackageObjectStandardName)
  }

  object ByPackageHeader {
    private def packageByNamespace(prefix:String) = PackageHeaderByNamespace(prefix, _.table.namespace)

    /**
      * Use this strategy when you want a separate source code file (or string)
      * for every single table. Typically you'll want to use this when table schemas are very large and
      * you want to minimize the footprint of your imports (i.e. since each file is a seperate table you
      * can be sure to just imports the exact tables needed for every source file).
      */
    def TablePerFile(packagePrefix:String = "") =
      PackagingStrategy(
        DoNotGroup,
        packageByNamespace(packagePrefix),
        packageByNamespace(packagePrefix),
        ByTable)

    /**
      * When you want each file (or string) to contain an entire schema, use this strategy.
      * This is useful for code-generators that have common code per-schema for example
      * the ComposeableTraitsGen that creates Traits representing database schemas that can
      * be composed with Contexts.
      */
    def TablePerSchema(packagePrefix:String = "") =
      PackagingStrategy(
        GroupByPackage,
        packageByNamespace(packagePrefix),
        packageByNamespace(packagePrefix),
        ByPackageName)

    def CommonCaseClassOriginalSchemas(packagePrefix:String = "") =
      PackagingStrategy(
        GroupByPackage,
        packageByNamespace(packagePrefix),
        PackageObjectByNamespace(packagePrefix, _.table.meta.head.tableSchem),
        ByTable)
  }

  def NoPackageCombined =
    PackagingStrategy(
      GroupByPackage,
      NoPackage,
      NoPackage,
      ByPackageName)

  def NoPackageSeparate =
    PackagingStrategy(
      DoNotGroup,
      NoPackage,
      NoPackage,
      ByPackageName)
}

case class PackagingStrategy(
  packageGroupingStrategy: PackageGroupingStrategy,
  packageNamingStrategyForCaseClasses: PackageNamingStrategy,
  packageNamingStrategyForQuerySchemas: PackageNamingStrategy,
  fileNamingStrategy:FileNamingStrategy
)

sealed trait PackageGroupingStrategy
case object GroupByPackage extends PackageGroupingStrategy
case object DoNotGroup extends PackageGroupingStrategy

sealed trait PackageNamingStrategy extends (TableStereotype => CodeWrapper)
object PackageNamingStrategy {
  type NamespaceMaker = TableStereotype => String
}
case class PackageHeaderByNamespace(val prefix:String, val namespaceMaker: PackageNamingStrategy.NamespaceMaker) extends PackageNamingStrategy with ByName {
  override def apply(table: TableStereotype): CodeWrapper = PackageHeader(byName(table))
}
case class PackageObjectByNamespace(val prefix:String, val namespaceMaker: PackageNamingStrategy.NamespaceMaker) extends PackageNamingStrategy with ByName {
  override def apply(table: TableStereotype): CodeWrapper = PackageObject(byName(table))
}
case class SimpleObjectByNamespace(val prefix:String, val namespaceMaker: PackageNamingStrategy.NamespaceMaker) extends PackageNamingStrategy with ByName {
  override def apply(table: TableStereotype): CodeWrapper = SimpleObject(byName(table))
}
case object NoPackage extends PackageNamingStrategy {
  override def apply(table: TableStereotype): CodeWrapper = NoWrapper
}



trait ByName {
  def prefix:String
  def namespaceMaker: PackageNamingStrategy.NamespaceMaker
  def byName(table: TableStereotype) = namespaceMaker(table)
}

sealed trait CodeWrapper
case class PackageHeader(packageName:String) extends CodeWrapper
case class PackageObject(packageName:String) extends CodeWrapper
case class SimpleObject(packageName:String) extends CodeWrapper
case object NoWrapper extends CodeWrapper

sealed trait FileNamingStrategy

/**
  * Name each package by the name of the table being generated.
  * If multiple tables are going to the generator, need to choose which one to use,
  * most likely the 1st. Typically used in ByPackage strategies. This is
  * the most common use-case.
  */
case object ByTable extends FileNamingStrategy

/**
  * Use this when generating package object so the filename will always be 'package.scala'
  */
case object ByPackageObjectStandardName extends FileNamingStrategy

/**
  * Typically used when multiple Tables are grouped into the same schema. but a package object is not used.
  */
case object ByPackageName extends FileNamingStrategy
case object ByDefaultName extends FileNamingStrategy

trait WithFileNaming {
  type Gen <: AbstractCodeEmitter
  case class BySomeTableData[Gen](namer:Gen => java.nio.file.Path) extends FileNamingStrategy
}

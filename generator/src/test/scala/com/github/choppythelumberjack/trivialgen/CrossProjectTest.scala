package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.ext.{AutoDiscoveringGen, ComposeableTraitsGen, MirrorContext}
import com.github.choppythelumberjack.trivialgen.model.StereotypingService.Namespacer
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

class CrossProjectTest extends SchemaUsingSpec {

  "composeable generator tests" - {

    "generate Composeable Schema in test project - trivial" in {
      val gen = new ComposeableTraitsGen(snakecaseConfig, pack(0), false) {
        override def namingStrategy: EntityNamingStrategy = TrivialSnakeCaseNames
      }
      gen.writeFiles(path(0))
    }

    "generate Composeable Schema in test project - simple" in {
      val gen = new ComposeableTraitsGen(snakecaseConfig, pack(1)) {
        override def namingStrategy: EntityNamingStrategy =
          CustomStrategy(
            col => col.columnName.toLowerCase.replace("_name", "")
          )
      }
      gen.writeFiles(path(1))
    }

    "generate Composeable Schema in test project - stereotyped one schema" in {
      val gen = new ComposeableTraitsGen(
        twoSchemaConfig, pack(2),
        nestedTrait = true)
      {
        override def namingStrategy: EntityNamingStrategy = CustomStrategy()
        override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
        override val namespacer: Namespacer = ts =>
          if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "public"
          else ts.tableSchem.toLowerCase
      }

      gen.writeFiles(path(2))
    }

    "generate Composeable Schema in test project - stereotyped multiple schemas" in {
      val gen = new ComposeableTraitsGen(twoSchemaConfig, pack(3), false)
      {
        override def namingStrategy: EntityNamingStrategy = CustomStrategy()
        override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
        override val namespacer: Namespacer =
          ts => if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "common" else ts.tableSchem.toLowerCase
      }

      gen.writeFiles(path(3))
    }

    "generate Composeable Schema in test project - non-stereotyped" in {
      val gen = new ComposeableTraitsGen(twoSchemaConfig, pack(4), nestedTrait = true) {
        override def namingStrategy: EntityNamingStrategy = CustomStrategy()
      }

      gen.writeFiles(path(4))
    }
  }

  "composeable auto discovering tests" - {
    "using mirror context - simple" in {
      val gen = new AutoDiscoveringGen(snakecaseConfig, MirrorContext, pack(5), false) {
        override def namingStrategy: EntityNamingStrategy = CustomStrategy()
      }

      gen.writeFiles(path(5))
    }

    "using mirror context - stereotyped multiple schemas" in {
      val gen = new AutoDiscoveringGen(twoSchemaConfig, MirrorContext, pack(6), false)
      {
        override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
        override def namingStrategy: EntityNamingStrategy = CustomStrategy()
        override val namespacer: Namespacer =
          ts => if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "common" else ts.tableSchem.toLowerCase
      }

      gen.writeFiles(path(6))
    }

    "generate Composeable Schema in test project - non-stereotyped" in {
      val gen = new AutoDiscoveringGen(twoSchemaConfig, MirrorContext, pack(7), nestedTrait = true) {
        override def namingStrategy: EntityNamingStrategy = CustomStrategy()
      }

      gen.writeFiles(path(7))
    }
  }

  // TODO Test for stereotyping in multiple databases
}

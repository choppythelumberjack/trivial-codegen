package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.codegen.CodegenSpec
import com.github.choppythelumberjack.trivialgen.util.StringUtil._
import com.typesafe.scalalogging.Logger

class StructuralTests extends CodegenSpec with HasStandardGen {

  def LOG = Logger(getClass)

  "simple end to end tests" - {

    "snake case naming strategy" - {
      val personData = fdgConv("id" -> "Int", "firstName" -> "Option[String]", "lastName" -> "Option[String]", "age" -> "Int")(_.lowerCamelToSnake.toUpperCase)
      val addressData = fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(_.lowerCamelToSnake.toUpperCase)

      "single table" in {
        val gens = standardGen(
          "src/test/resources/schema_snakecase.sql",
          _.table.tableName.toLowerCase == "person",
          SnakeCaseNames
        ).makeGenerators.toList

        LOG.info(gens(0).tableSchemasCode)

        assertCaseClass(gens(0).caseClassesCode, "Person", personData.ccList)
        assertStandardObject(gens(0).tableSchemasCode,
          "Person", "Person", Seq(QuerySchema("person", "PUBLIC.PERSON", List()))
        )
      }

      "multi table" in {

        val gens = standardGen(
          "src/test/resources/schema_snakecase.sql",
          entityNamingStrategy = SnakeCaseNames
        ).makeGenerators.toList.sortBy(_.caseClassesCode)

        assertCaseClass(gens(0).caseClassesCode, "Address", addressData.ccList)
        assertStandardObject(gens(0).tableSchemasCode,
          "Address", "Address", Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
        )

        assertCaseClass(gens(1).caseClassesCode, "Person", personData.ccList)
        assertStandardObject(gens(1).tableSchemasCode,
          "Person", "Person", Seq(QuerySchema("person", "PUBLIC.PERSON", List()))
        )
      }
    }

    "custom naming strateogy" - {

      val personData = fdgConv("id" -> "Int", "firstname" -> "Option[String]", "lastname" -> "Option[String]", "age" -> "Int")(_.toUpperCase)
      val addressData = fdgConv("personfk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(_.toUpperCase)

      "single table" in {

        val gens = standardGen(
          "src/test/resources/schema.sql",
          _.table.tableName.toLowerCase == "person",
          CustomStrategy(c => c.columnName.toLowerCase, s => s.tableName.toLowerCase)
        ).makeGenerators.toList

        assertCaseClass(gens(0).caseClassesCode, "person", personData.ccList)
        assertStandardObject(gens(0).tableSchemasCode,
          "person", "person", Seq(QuerySchema("person", "PUBLIC.PERSON", personData.querySchemaList))
        )
      }

      "multi table" in {

        val gens = standardGen(
          "src/test/resources/schema.sql",
          entityNamingStrategy = CustomStrategy(c => c.columnName.toLowerCase, s => s.tableName.toLowerCase)
        ).makeGenerators.toList.sortBy(_.caseClassesCode)

        assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
        assertStandardObject(gens(0).tableSchemasCode,
          "address", "address", Seq(QuerySchema("address", "PUBLIC.ADDRESS", addressData.querySchemaList))
        )

        assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
        assertStandardObject(gens(1).tableSchemasCode,
          "person", "person", Seq(QuerySchema("person", "PUBLIC.PERSON", personData.querySchemaList))
        )
      }
    }

  }

  "collision end to end tests" - {

    "custom naming" - {
      val personData = fdgConv("id" -> "Int", "firstname" -> "Option[String]", "lastname" -> "Option[String]", "age" -> "Int")(_.toUpperCase)
      val addressData = fdgConv("personfk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(_.toUpperCase)

      "prefix collision" in {
        val gens = standardGen(
          "src/test/resources/schema_twotable.sql",
          entityNamingStrategy = CustomStrategy(
            c => c.columnName.toLowerCase,
            s => {
              s.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", "")
            }
          )
        ).makeGenerators.toList.sortBy(_.caseClassesCode)

        assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
        assertStandardObject(gens(0).tableSchemasCode,
          "address", "address", Seq(QuerySchema("address", "PUBLIC.ADDRESS", addressData.querySchemaList))
        )

        assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
        assertStandardObject(gens(1).tableSchemasCode,
          "person", "person", Seq(
            QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", personData.querySchemaList),
            QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", personData.querySchemaList)
          )
        )
      }
    }

    "with snake schema" - {

      "prefix collision - different columns without datatype perculation" - {

        val personData = fdgConv("id" -> "Int", "firstName" -> "Option[String]", "lastName" -> "Option[String]", "age" -> "Int")(_.lowerCamelToSnake.toUpperCase)
        val addressData = fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(_.lowerCamelToSnake.toUpperCase)

        "prefix test with snake case" in {
          val gens = standardGen(
            "src/test/resources/schema_snakecase_twotable.sql",
            entityNamingStrategy = SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", ""))
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
          assertStandardObject(gens(0).tableSchemasCode,
            "address", "address", Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
          assertStandardObject(gens(1).tableSchemasCode,
            "person", "person", Seq(
              QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", List()),
              QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", List())
            )
          )
        }

        "prefix collision - different columns with datatype perculation" in {
          val gens = standardGen(
            "src/test/resources/schema_snakecase_twotable_differentcolumns.sql",
            entityNamingStrategy = SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", ""))
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
          assertStandardObject(gens(0).tableSchemasCode,
            "address", "address", Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
          assertStandardObject(gens(1).tableSchemasCode,
            "person", "person", Seq(
              QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", List()),
              QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", List())
            )
          )
        }
      }

      "prefix collision - different columns with datatype perculation" - {

        val personData = fdgConv("id" -> "Int", "firstName" -> "Option[String]", "lastName" -> "Option[String]", "age" -> "Int",
          "numTrinkets" -> "Option[Long]", "trinketType" -> "String")(_.lowerCamelToSnake.toUpperCase)

        val addressData = fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Int")(_.lowerCamelToSnake.toUpperCase)

        "prefix test with snake case - with different columns - and different types" in {
          val gens = standardGen(
            "src/test/resources/schema_snakecase_twotable_differentcolumns_differenttypes.sql",
            entityNamingStrategy = SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", "").capitalize)
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "Address", addressData.ccList)
          assertStandardObject(gens(0).tableSchemasCode,
            "Address", "Address", Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "Person", personData.ccList)
          assertStandardObject(gens(1).tableSchemasCode,
            "Person", "Person", Seq(
              QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", List()),
              QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", List())
            )
          )
        }
      }

      "namespace collision - different columns with datatype perculation" - {

        val personData = fdgConv("id" -> "Int", "firstName" -> "Option[String]", "lastName" -> "Option[String]", "age" -> "Int",
          "numTrinkets" -> "Option[Long]", "trinketType" -> "String")(_.lowerCamelToSnake.toUpperCase)

        val addressData = fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Int")(_.lowerCamelToSnake.toUpperCase)

        "prefix test with snake case - with different columns - and different types" in {
          val gens = standardGen(
            "src/test/resources/schema_snakecase_twoschema_differentcolumns_differenttypes.sql",
            entityNamingStrategy = SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", "").capitalize),
            entityNamespacer = _.tableSchem.toLowerCase.replaceAll("(alpha)|(bravo)", "public"),
            entityMemberNamer = ts => s"${ts.tableSchem}_${ts.tableName}".toLowerCase.snakeToLowerCamel
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "Address", addressData.ccList)
          assertStandardObject(gens(0).tableSchemasCode,
            "Address", "Address", Seq(QuerySchema("publicAddress", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "Person", personData.ccList)
          assertStandardObject(gens(1).tableSchemasCode,
            "Person", "Person", Seq(
              QuerySchema("alphaPerson", "ALPHA.PERSON", List()),
              QuerySchema("bravoPerson", "BRAVO.PERSON", List())
            )
          )
        }
      }
    }
  }
}

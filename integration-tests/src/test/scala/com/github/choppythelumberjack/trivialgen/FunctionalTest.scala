package com.github.choppythelumberjack.trivialgen

import java.io.Closeable
import java.sql.ResultSet
import javax.sql.DataSource

import com.github.choppythelumberjack.tryclose.{Failure, Success, TryClose}
import io.getquill._
import org.h2.jdbcx.JdbcDataSource
import org.scalatest.{FreeSpec, Matchers}

class FunctionalTest extends SchemaFunctionTestSpec {

  "composeable generator tests" - {
    /**
      * Technically this first test does not test the composition of the trait since no
      * composeable trait will be created since there are no query schemas. However,
      * the sanity check is that case classes that are useable for a query will still be generated.
      */
    "generate Composeable Schema in test project - trivial sanity test" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp0.public._

      val ctx = new H2JdbcContext[SnakeCase](SnakeCase, snakecaseDS)
      import ctx._

      val results = ctx.run(query[Person].filter(_.age > 11)).toSeq
      results should contain theSameElementsAs
        (List(Person(1, "Joe".?, "Bloggs".?, 22), Person(2, "Jack".?, "Ripper".?, 33)))
    }

    "generate Composeable Schema in test project - simple" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp1.public._

      val ctx = new H2JdbcContext[Literal](Literal, snakecaseDS)
        with PublicExtensions[H2Dialect, Literal]
      import ctx._

      val results = ctx.run(PersonDao.query.filter(_.age > 11)).toSeq
      results should contain theSameElementsAs
        (List(Person(1, "Joe".?, "Bloggs".?, 22), Person(2, "Jack".?, "Ripper".?, 33)))
    }

    "generate Composeable Schema in test project - stereotyped one schema" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp2.public._

      val ctx = new H2JdbcContext[Literal](Literal, twoSchemaDS)
        with PublicExtensions[H2Dialect, Literal]
      import ctx._

      val results = ctx.run(PublicSchema.PersonDao.alphaPerson.filter(_.age > 11)).toSeq
      results should contain theSameElementsAs (
        (List(Person(1, "Joe".?, "Bloggs".?, 22, 55L.?, "Wonkles"), Person(2, "Jack".?, "Ripper".?, 33, 66L.?, "Ginkles"))))
    }

    "generate Composeable Schema in test project - stereotyped multiple schemas" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp3.common._
      import com.github.choppythelumberjack.trivialgen.generated.comp3.public._

      val ctx = new H2JdbcContext[Literal](Literal, twoSchemaDS)
        with PublicExtensions[H2Dialect, Literal] with CommonExtensions[H2Dialect, Literal]
      import ctx._

      val results = ctx.run(PersonDao.alphaPerson.filter(_.age > 11)).toSeq
      results should contain theSameElementsAs (
        (List(Person(1, "Joe".?, "Bloggs".?, 22, 55L.?, "Wonkles"), Person(2, "Jack".?, "Ripper".?, 33, 66L.?, "Ginkles"))))
    }

    "generate Composeable Schema in test project - non-stereotyped" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp4._

      val ctx = new H2JdbcContext[Literal](Literal, twoSchemaDS)
        with alpha.AlphaExtensions[H2Dialect, Literal] with bravo.BravoExtensions[H2Dialect, Literal] with public.PublicExtensions[H2Dialect, Literal]
      import com.github.choppythelumberjack.trivialgen.generated.comp4.alpha.Person
      import ctx._

      val results = ctx.run(ctx.AlphaSchema.PersonDao.query.filter(_.age > 11)).toSeq
      results should contain theSameElementsAs (
        (List(
          Person(1, "Joe".?, "Bloggs".?, 22, "blah".?, 55.?, "Wonkles"),
          Person(2, "Jack".?, "Ripper".?, 33, "blah".?, 66.?, "Ginkles"))))
    }
  }

  "composeable auto discovering tests" - {
    import MapMakingUtils._

    "using mirror context - simple" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp5._
      import CombinedContext._

      val results = CombinedContext.run(PersonDao.query.filter(_.age > 11)).string
      results.execute(snakecaseDS) should contain theSameElementsAs
        (List(new public.Person(1, "Joe".?, "Bloggs".?, 22), public.Person(2, "Jack".?, "Ripper".?, 33)).toMapsUnpacked)
    }
    "using mirror context - stereotyped multiple schemas" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp6._
      import CombinedContext._

      val results = CombinedContext.run(PersonDao.alphaPerson.filter(_.age > 11)).string
      results.execute(twoSchemaDS) should contain theSameElementsAs (
        (List(
          common.Person(1, "Joe".?, "Bloggs".?, 22, 55L.?, "Wonkles"),
          common.Person(2, "Jack".?, "Ripper".?, 33, 66L.?, "Ginkles"))).toMapsUnpacked)
    }
    "generate Composeable Schema in test project - non-stereotyped" in {
      import com.github.choppythelumberjack.trivialgen.generated.comp7._
      import CombinedContext._

      val results = CombinedContext.run(AlphaSchema.PersonDao.query.filter(_.age > 11)).string
      results.execute(twoSchemaDS) should contain theSameElementsAs (
        (List(
          alpha.Person(1, "Joe".?, "Bloggs".?, 22, "blah".?, 55.?, "Wonkles"),
          alpha.Person(2, "Jack".?, "Ripper".?, 33, "blah".?, 66.?, "Ginkles"))).toMapsUnpacked)
    }
  }
}

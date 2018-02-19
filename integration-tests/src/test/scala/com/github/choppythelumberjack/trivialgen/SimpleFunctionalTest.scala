package com.github.choppythelumberjack.trivialgen

import io.getquill._

class SimpleFunctionalTest extends SchemaFunctionTestSpec {

  "trivial generator tests" - {
    "use trivial snake case schema" in {
      import com.github.choppythelumberjack.trivialgen.generated.simp0.public._

      val ctx = new H2JdbcContext[SnakeCase](SnakeCase, snakecaseDS)
      import ctx._

      val results = ctx.run(query[Person].filter(_.age > 11)).toSeq
      results should contain theSameElementsAs
        (List(Person(1, "Joe".?, "Bloggs".?, 22), Person(2, "Jack".?, "Ripper".?, 33)))
    }
    "use trivial literal schema" in {
      import com.github.choppythelumberjack.trivialgen.generated.simp1.public._

      val ctx = new H2JdbcContext[Escape](Escape, literalDS)
      import ctx._

      val results = ctx.run(query[Person].filter(_.age > 11)).toSeq
      results should contain theSameElementsAs
        (List(Person(1, "Joe".?, "Bloggs".?, 22), Person(2, "Jack".?, "Ripper".?, 33)))
    }
  }
}

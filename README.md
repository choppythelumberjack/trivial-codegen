# Trivial Quill Code Generator

This library is designed to give you a few options as to what kind of 
schema to generate from JDBC metadata for quill. You can choose to generate
either simple case classes or a combination of case classes and query schemas,
as well as whether they should written to one file, multiple files, or
just a list of strings (useful for streaming directly into a repl).
Inspired by the Slick code generator but albeit a bit simpler,
the code generator is fairly customizeable although a couple of
ready-to-use implementations come out of the box. 

## Trivial Gen

The Trivial Generator is the simplest code generator in this library. 
The purpose of the trivial code generator is to generate simple case classes representing tables
in a database. Create one or multiple CodeGeneratorConfig objects
and call the `.writeFiles` or `.writeStrings` methods
on the code generator and the rest happens automatically.

Given the following schema:
````sql
create table Person (
  id int primary key auto_increment,
  first_name varchar(255),
  last_name varchar(255),
  age int not null
);

create table Address (
  person_fk int not null,
  street varchar(255),
  zip int
);
````


You can invoke the Trivial Generator like so:

````scala
val gen = new TrivialGen(snakecaseConfig, "com.github.choppythelumberjack.trivialgen.generated.simp0") {
    override def namingStrategy = TrivialSnakeCaseNames
}
gen.writeFiles("integration-tests/src/test/scala/com/github/choppythelumberjack/trivialgen/generated/simp0")
````

Note that there are two options for the Trivial Generator's Naming strategy, the `TrivialSnakeCaseNames` strategy
and the `TrivialLiteralNames` strategy. In the former case, schemas will
be converted to lower case before translation. Use the appropriate Quill Naming Strategy
if your database is case sensitive. (Neither one of them allow custom naming of table or columns. Use the
ComposeableTraitsGen to get that functionality)

The following case case classes will be generated
````scala
// src/test/scala/com/github/choppythelumberjack/trivialgen/generated/simp0/public/Person.scala
package com.github.choppythelumberjack.trivialgen.generated.simp0.public
  
case class Person(id: Int, firstName: Option[String], lastName: Option[String], age: Int)
````

````scala
// src/test/scala/com/github/choppythelumberjack/trivialgen/generated/simp0/public/Address.scala
package com.github.choppythelumberjack.trivialgen.generated.simp0.public
  
case class Address(personFk: Int, street: Option[String], zip: Option[Int])
````


If you wish to generate schemas with custom table or column names, you need to use the ComposeableTraitsGen
in order to generate your schemas.

## Composeable Traits Gen

The composeable traits generator allows you to custom-name any table and/or column and generates
quill `querySchema` objects that translate the columns names. Additionally, it generates a 
database-independent query schema trait which can be composed with a custom context
that you can later create in your client code.

Given the following schema:
````sql
create table Person (
  id int primary key auto_increment,
  first_name varchar(255),
  last_name varchar(255),
  age int not null
);

create table Address (
  person_fk int not null,
  street varchar(255),
  zip int
);
````

Here is a example of how you could use the `ComposeableTraitsGen` in order to replace the
`first_name` and `last_name` properties with `first` and `last`.

````scala
val gen = new ComposeableTraitsGen(
snakecaseConfig,
"com.github.choppythelumberjack.trivialgen.generated.comp1"
) {
override def namingStrategy: EntityNamingStrategy =
  CustomStrategy(
    col => col.columnName.toLowerCase.replace("_name", "")
  )
}
gen.writeFiles("integration-tests/src/test/scala/com/github/choppythelumberjack/trivialgen/generated/comp1")
````
 

The following schema should be generated as a result.
````
package com.github.choppythelumberjack.trivialgen.generated.comp1.public

case class Address(person_fk: Int, street: Option[String], zip: Option[Int])
case class Person(id: Int, first: Option[String], last: Option[String], age: Int)

// Note that by default this is formatted as "${namespace}Extensions"
trait PublicExtensions[Idiom <: io.getquill.idiom.Idiom, Naming <: io.getquill.NamingStrategy] {
  this:io.getquill.context.Context[Idiom, Naming] =>

  object AddressDao {
      def query = quote {
          querySchema[Address](
            "PUBLIC.ADDRESS",
            _.person_fk -> "PERSON_FK",
            _.street -> "STREET",
            _.zip -> "ZIP"
          )
        }
    }

    object PersonDao {
      def query = quote {
          querySchema[Person](
            "PUBLIC.PERSON",
            _.id -> "ID",
            _.first -> "FIRST_NAME",
            _.last -> "LAST_NAME",
            _.age -> "AGE"
          )
        }
    }
}
````

Later when declaring your quill database context you can compose the context with
the `PublicExtensions` like so:
````
object MyCustomContext extends SqlMirrorContext[H2Dialect, Literal](H2Dialect, Literal)
  with PublicExtensions[H2Dialect, Literal]
````


### Stereotyping
There are times you will encounter the same kind of table across multiple schemas, multiple databases
or perhaps just prefixed differently. Typically, this table will have almost the same columns
and the same datatypes of those columns except just one or two.

Examine the following H2 DDL:
````sql
create table Alpha.Person (
  id int primary key auto_increment,
  first_name varchar(255),
  last_name varchar(255),
  age int not null,
  foo varchar(255),
  num_trinkets int,
  trinket_type varchar(255) not null
);

create table Bravo.Person (
  id int primary key auto_increment,
  first_name varchar(255),
  bar varchar(255),
  last_name varchar(255),
  age int not null,
  num_trinkets bigint not null,
  trinket_type int not null
);
````

 * Firstly, note that `Alpha.Person` and `Bravo.Person` have the exact same columns except for `foo` and `bar` respectively.
   If a common table definition `Person` is desired, these columns must be omitted.
 * Secondly, note that their columns `num_trinkets` and `trinket_type` have different datatypes.
   If a common table definition `Person` is desired, these columns must be expanded to the widest
   datatype of the two which is this case `bigint` for `num_trinkets` and `varchar(255)` for `trinket_type`.  

Both of the above actions are automatically performed by the `ComposeableTraitsGen`
(as well as the Trivial Gen if needed) automatically when multiple tables are renamed to the same thing.
Here is an example of how that is done:

````scala
val gen = new ComposeableTraitsGen(
  twoSchemaConfig, 
  "com.github.choppythelumberjack.trivialgen.generated.comp1"
) {
  override def namingStrategy: EntityNamingStrategy = CustomStrategy()
  override val namespacer: Namespacer =
    ts => if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "common" else ts.tableSchem.toLowerCase
    
  // Be sure to set the memberNamer correctly so that the different
  // querySchemas generated won't all be called '.query' in the common object.
  override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
}

gen.writeFiles(path(3))
````

The following will then be generated. Note how `numTrinkets` is a `Long` (i.e. an SQL `bigint`) type and `trinketType` is a `String`
(i.e. an SQL varchar),

````scala
package com.github.choppythelumberjack.trivialgen.generated.comp3.common
  
case class Person(id: Int, firstName: Option[String], lastName: Option[String], age: Int, numTrinkets: Option[Long], trinketType: String)
  
trait CommonExtensions[Idiom <: io.getquill.idiom.Idiom, Naming <: io.getquill.NamingStrategy] {
  this:io.getquill.context.Context[Idiom, Naming] =>
  
  object PersonDao {
      def alphaPerson = quote {
          querySchema[Person](
            "ALPHA.PERSON",
            _.id -> "ID",
            _.firstName -> "FIRST_NAME",
            _.lastName -> "LAST_NAME",
            _.age -> "AGE",
            _.numTrinkets -> "NUM_TRINKETS",
            _.trinketType -> "TRINKET_TYPE"
          )
        }
  
      def bravoPerson = quote {
          querySchema[Person](
            "BRAVO.PERSON",
            _.id -> "ID",
            _.firstName -> "FIRST_NAME",
            _.lastName -> "LAST_NAME",
            _.age -> "AGE",
            _.numTrinkets -> "NUM_TRINKETS",
            _.trinketType -> "TRINKET_TYPE"
          )
        }
    }
}
 
````

Later when declaring your quill database context you can compose the context with
the `CommonExtensions` like so:
````
object MyCustomContext extends SqlMirrorContext[H2Dialect, Literal](H2Dialect, Literal)
  with CommonExtensions[H2Dialect, Literal]
````
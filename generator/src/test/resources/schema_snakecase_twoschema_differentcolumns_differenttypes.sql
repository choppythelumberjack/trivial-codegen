DROP ALL OBJECTS;

CREATE SCHEMA IF NOT EXISTS Alpha;
CREATE SCHEMA IF NOT EXISTS Bravo;

create table Alpha.Person (
  id int primary key auto_increment,
  first_name varchar(255),
  last_name varchar(255),
  age int not null,
  foo varchar(255),
  num_trinkets int, --The other one is a bigint so output needs to be a bigint (i.e. Long) but this one is nullable so output must be nullable (i.e. Option)
  trinket_type varchar(255) not null --Other one is an int but have to make datatype String since this one is a string. Since neither is nullable don't need Option.
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

create table Address (
  person_fk int not null,
  street varchar(255),
  zip int not null
);

insert into Alpha.Person values (default, 'Joe', 'Bloggs', 22, 'blah', 55, 'Wonkles');
insert into Alpha.Person values (default, 'Jack', 'Ripper', 33, 'blah', 66, 'Ginkles');

insert into Bravo.Person values (default, 'George', 'blah', 'Oleaf', 22, 77, 1);
insert into Bravo.Person values (default, 'Greg', 'blah', 'Raynor', 33, 88, 2);

insert into Address values (1, '123 Someplace', 1001);
insert into Address values (1, '678 Blah', 2002);
insert into Address values (2, '111234 Some Other Place', 3333);

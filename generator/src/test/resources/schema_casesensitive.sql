DROP ALL OBJECTS;

create table "Person" (
  "id" int primary key auto_increment,
  "firstName" varchar(255),
  "lastName" varchar(255),
  "age" int not null
);

create table "Address" (
  "personFk" int not null,
  "street" varchar(255),
  "zip" int
);

insert into "Person" values (default, 'Joe', 'Bloggs', 22);
insert into "Person" values (default, 'Jack', 'Ripper', 33);
insert into "Address" values (1, '123 Someplace', 1001);
insert into "Address" values (1, '678 Blah', 2002);
insert into "Address" values (2, '111234 Some Other Place', 3333);

create table if not exists rooms
(
  id integer primary key,
  name string,
  grossdescription string,
  northexit int,
  eastexit int,
  southexit int,
  westexit int
);

insert into rooms (id, name, grossdescription, southexit, eastexit) values
(
  1,
  "a room",
  "This is a room. It might or might not have anything in it.",
  2,
  4
);

insert into rooms (id, name, grossdescription, northexit, eastexit) values
(
  2,
  "a second room",
  "This is also a room. Again, there might be things here.",
  1,
  3
);
  
insert into rooms (id, name, grossdescription, westexit, northexit) values
(
  3,
  "the southeast room",
  "This is a room in the southeast corner.",
  2,
  4
);

insert into rooms (id, name, grossdescription, westexit, southexit) values
(
  4,
  "the northeast room",
  "This is a room in the northeast corner.",
  1,
  3
);
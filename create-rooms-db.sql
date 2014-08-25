create table if not exists rooms
(
  name string,
  grossdescription string,
  northexit int,
  eastexit int,
  southexit int,
  westexit int
);

insert into rooms (rowid, name, grossdescription, southexit) values
(
  1,
  "a room",
  "This is a room. It might or might not have anything in it.",
  2
);

insert into rooms (rowid, name, grossdescription, northexit) values
(
  2,
  "a second room",
  "This is also a room. Again, there might be things here.",
  1
);
  
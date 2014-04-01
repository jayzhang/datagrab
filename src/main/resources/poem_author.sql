drop table if exists author;
create table author (
id TEXT PRIMARY KEY
, name TEXT
, alph TEXT
, era TEXT
, desp TEXT
, poemnum INTEGER);
CREATE INDEX alph_index ON author (alph);

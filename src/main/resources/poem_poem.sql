drop table if exists poem;
create table poem (
id TEXT PRIMARY KEY
, title TEXT
, author TEXT
, content TEXT);
CREATE INDEX author_index ON poem (author);
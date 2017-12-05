drop table if exists jian;

CREATE TABLE [jian] (
  [card] INT, 
  [gui] INT, 
  [jiang] INT, 
  [hu] INT);


CREATE INDEX [cardjiang]
ON [jian](
    [card]);INSERT INTO jian( card, gui, jiang, hu) VALUES (10, 8, 0, -1);
INSERT INTO jian( card, gui, jiang, hu) VALUES (10, 0, 1, 10);
INSERT INTO jian( card, gui, jiang, hu) VALUES (10, 5, 0, -1);

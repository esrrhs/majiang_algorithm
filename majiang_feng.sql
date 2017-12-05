drop table if exists feng;

CREATE TABLE [feng] (
  [card] INT, 
  [gui] INT, 
  [jiang] INT, 
  [hu] INT);


CREATE INDEX [cardfeng]
ON [feng](
    [card]);INSERT INTO feng( card, gui, jiang, hu) VALUES (1, 8, 0, -1);
INSERT INTO feng( card, gui, jiang, hu) VALUES (1, 0, 1, 1);

drop table if exists normal;

CREATE TABLE [normal] (
  [card] INT, 
  [gui] INT, 
  [jiang] INT, 
  [hu] INT);


CREATE INDEX [card]
ON [normal](
    [card]);INSERT INTO normal( card, gui, jiang, hu) VALUES (303043100, 0, 0, 1000);
INSERT INTO normal( card, gui, jiang, hu) VALUES (303043100, 0, 1, -1);

CREATE TABLE IF NOT EXISTS `user` (
    `id`   INTEGER      NOT NULL AUTO_INCREMENT,
    `first_name` VARCHAR(128) NOT NULL,
    `last_name` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `new_user` (
    `id`   INTEGER      NOT NULL AUTO_INCREMENT,
    `first_name` VARCHAR(128) NOT NULL,
    `last_name` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`id`)
);

TRUNCATE TABLE user;
TRUNCATE TABLE new_user;

INSERT INTO user (id, first_name, last_name) VALUES
  (1, 'Evie', 'Lim'),
  (2, 'Elana', 'Nielsen'),
  (3, 'Rebekah', 'Iles'),
  (4, 'Davina', 'Cook'),
  (5, 'Dave', 'Sharpe'),
  (6, 'Danyl', 'Terry'),
  (7, 'Bethaney', 'Hogg');

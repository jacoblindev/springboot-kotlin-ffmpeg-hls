CREATE TABLE IF NOT EXISTS videos (
    id       VARCHAR(64)  DEFAULT RANDOM_UUID() PRIMARY KEY,
    title    VARCHAR(64)  NOT NULL,
    poster   VARCHAR      NOT NULL,
    playlist VARCHAR      NOT NULL
);
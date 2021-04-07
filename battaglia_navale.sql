DROP DATABASE IF EXISTS battaglia_navale;
CREATE DATABASE	battaglia_navale;
USE battaglia_navale;

CREATE TABLE account(
  Nickname varchar(30) PRIMARY KEY NOT NULL,	
  password varchar(128) NOT NULL,
  statoGiocatore boolean NOT NULL,
  Inpartita boolean NOT NULL
);

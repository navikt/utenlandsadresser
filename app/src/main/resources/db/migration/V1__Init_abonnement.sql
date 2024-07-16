CREATE TABLE abonnement
(
    organisasjonsnummer TEXT      NOT NULL,
    identitetsnummer    TEXT      NOT NULL,
    opprettet           TIMESTAMP NOT NULL,
    PRIMARY KEY (identitetsnummer, organisasjonsnummer)
);

CREATE TABLE feed
(
    organisasjonsnummer TEXT      NOT NULL,
    "løpenummer"        INT       NOT NULL,
    identitetsnummer    TEXT      NOT NULL,
    opprettet           TIMESTAMP NOT NULL,
    PRIMARY KEY (identitetsnummer, "løpenummer", organisasjonsnummer)
);

CREATE INDEX "feed_løpenummer_organisasjonsnummer" ON feed ("løpenummer" DESC, organisasjonsnummer);

CREATE TABLE abonnement (
    client_id TEXT NOT NULL,
    identitetsnummer CHAR(11) NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    PRIMARY KEY (identitetsnummer, client_id)
);

CREATE TABLE feed (
    client_id TEXT NOT NULL,
    "løpenummer" INT NOT NULL,
    identitetsnummer CHAR(11) NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    PRIMARY KEY (identitetsnummer, "løpenummer", client_id)
);

CREATE INDEX "feed_løpenummer_client_id" ON feed ("løpenummer" DESC, client_id);

CREATE TABLE abonnement (
    "fødselsnummer" CHARACTER(11) NOT NULL,
    client_id TEXT NOT NULL,
    "løpenummer" INTEGER NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    PRIMARY KEY ("fødselsnummer", client_id),
);

CREATE INDEX "abonnement_fødselsnummer" ON abonnement ("fødselsnummer");

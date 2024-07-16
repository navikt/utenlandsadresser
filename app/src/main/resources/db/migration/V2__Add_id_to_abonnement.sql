ALTER TABLE abonnement
    ADD COLUMN id UUID;

UPDATE abonnement
SET id = gen_random_uuid()
WHERE id IS NULL;

ALTER TABLE abonnement
    DROP CONSTRAINT abonnement_pkey;

ALTER TABLE abonnement
    ADD PRIMARY KEY (id);

ALTER TABLE abonnement
    ADD CONSTRAINT unique_abonnement UNIQUE (organisasjonsnummer, identitetsnummer);

CREATE INDEX identitetsnummer_index ON abonnement (identitetsnummer);

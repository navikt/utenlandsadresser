CREATE TABLE sporingslogg
(
    id                       SERIAL PRIMARY KEY,
    identitetsnummer         TEXT      NOT NULL,
    mottaker                 TEXT      NOT NULL,
    utlevert_data            jsonb     NOT NULL,
    tidspunkt_for_utlevering TIMESTAMP NOT NULL DEFAULT NOW()
)
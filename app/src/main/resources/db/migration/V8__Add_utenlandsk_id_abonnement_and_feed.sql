CREATE TABLE utenlandsk_id_abonnement (
    id UUID PRIMARY KEY,
    organisasjonsnummer TEXT NOT NULL,
    identitetsnummer TEXT NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    CONSTRAINT unique_utenlandsk_id_abonnement UNIQUE (organisasjonsnummer, identitetsnummer)
);

CREATE INDEX utenlandsk_id_abonnement_identitetsnummer_index ON utenlandsk_id_abonnement (identitetsnummer);

-- Primærnøkkelen sikrer at et løpenummer bare brukes én gang per mottaker.
-- Ingen fremmednøkkel til abonnement: hendelser skal bli liggende når et abonnement stoppes,
-- ellers oppstår hull i løpenummerrekken.
CREATE TABLE utenlandsk_id_feed (
    organisasjonsnummer TEXT NOT NULL,
    "løpenummer" INT NOT NULL,
    identitetsnummer TEXT NOT NULL,
    abonnement_id UUID NOT NULL,
    hendelsestype INTEGER NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    PRIMARY KEY (organisasjonsnummer, "løpenummer")
);

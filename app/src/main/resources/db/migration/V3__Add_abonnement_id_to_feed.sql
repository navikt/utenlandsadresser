ALTER TABLE feed
    ADD COLUMN abonnement_id UUID;

UPDATE feed
SET abonnement_id = abonnement.id
FROM abonnement
WHERE feed.organisasjonsnummer = abonnement.organisasjonsnummer
  AND feed.identitetsnummer = abonnement.identitetsnummer;

ALTER TABLE feed
    ALTER COLUMN abonnement_id SET NOT NULL;

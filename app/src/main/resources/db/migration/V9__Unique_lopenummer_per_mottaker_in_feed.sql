-- Løpenummer ble tildelt som max + 1 uten lås, så samtidige innsettinger kunne gi samme løpenummer til ulike
-- personer hos samme mottaker. Bare én av dem ble levert. Denne migreringen rydder opp og gjør
-- (organisasjonsnummer, løpenummer) til primærnøkkel.
--
-- Opprydningen kan ikke rulles tilbake. Den gamle primærnøkkelen kan gjenopprettes i en ny migrering.

-- ACCESS EXCLUSIVE med én gang. Å oppgradere fra en svakere lås før ADD PRIMARY KEY kan gi vranglås med
-- transaksjoner i gamle poder som har lest fra feed og venter på å skrive.
LOCK TABLE feed IN ACCESS EXCLUSIVE MODE;

CREATE TEMPORARY TABLE feed_duplikat ON COMMIT DROP AS
SELECT f.ctid AS rad, f.*
FROM feed f
WHERE (f.organisasjonsnummer, f."løpenummer") IN (
    SELECT organisasjonsnummer, "løpenummer"
    FROM feed
    GROUP BY organisasjonsnummer, "løpenummer"
    HAVING count(*) > 1
);

-- Vi vet ikke hvilken rad i en duplikatgruppe som ble levert, så alle legges bakerst i feeden.
-- Bare hendelser for abonnementer som fortsatt finnes kopieres, ellers deler vi adresser uten grunnlag.
INSERT INTO feed (organisasjonsnummer, "løpenummer", identitetsnummer, opprettet, abonnement_id, hendelsestype)
SELECT d.organisasjonsnummer,
       h.hoyeste + row_number() OVER (
           PARTITION BY d.organisasjonsnummer
           ORDER BY d."løpenummer", d.opprettet, d.identitetsnummer
       ),
       d.identitetsnummer,
       d.opprettet,
       d.abonnement_id,
       d.hendelsestype
FROM feed_duplikat d
JOIN abonnement a ON a.id = d.abonnement_id
JOIN (
    SELECT organisasjonsnummer, max("løpenummer") AS hoyeste
    FROM feed
    GROUP BY organisasjonsnummer
) h ON h.organisasjonsnummer = d.organisasjonsnummer;

-- Behold én rad per løpenummer, så feeden ikke får hull.
DELETE FROM feed f
USING (
    SELECT rad,
           row_number() OVER (
               PARTITION BY organisasjonsnummer, "løpenummer"
               ORDER BY opprettet, identitetsnummer
           ) AS nr
    FROM feed_duplikat
) d
WHERE f.ctid = d.rad
  AND d.nr > 1;

ALTER TABLE feed DROP CONSTRAINT feed_pkey;
ALTER TABLE feed ADD PRIMARY KEY (organisasjonsnummer, "løpenummer");

-- Primærnøkkelen dekker nå oppslag på organisasjonsnummer og løpenummer.
DROP INDEX "feed_løpenummer_organisasjonsnummer";

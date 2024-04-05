# utenlandsadresser
Deling av utenladsadresser med Skatteetaten.

## Beskrivelse av tjenesten

Tjenesten tilgjengeligjør et grensesnitt for å dele utenlandsadresser. Løsningen er en abonnementstjeneste der konsumenter kan abonnere på endringer i utenlandsadresser.
Etter å ha startet et abonnement vil konsumenten kunne lese endringer på en utenlandsadresse fra et feed-endepunkt.
Om det finnes en adresse når abonnementet startes, vil denne adressen bli lagt på feeden med en gang.
Tjenesten lytter etter endringer på en persons postadresse gjennom en Kafkastrøm fra PDL og putter hendelse på feed.

### Adressebeskyttelse

Adressebeskyttede adresser vil ikke bli delt.
Om en adresse blir adressebeskyttet vil konsumenter av tjenesten få en hendelse om at adressen skal slettes.
Det er da opp til konsumenten å slette adressen fra sin database.
Videre spørringer om adressen vil returnere tomme resultater.

## Beslutningslogg

- For å ikke duplisere logikk valgte vi å gjennbruke logikken til Team Dokumenthåndtering for å velge postadresse. Dette gjøres gjennom Registeroppslagt-APIet. Det andre alternativet vi vurderte var å koble oss direkte på PDLs GrapQL-grensesnitt.
- Adresser vi har delt med Skatteetaten lages in en SQL-database som sporingslogg. Sporingsloggen kan brukes om vi trenger å gi ut innsyn til hvilke adresser som er delt med Skatteetaten.

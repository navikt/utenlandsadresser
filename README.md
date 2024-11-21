# utenlandsadresser
Deling av utenladsadresser med Skatteetaten.

![Design](https://github.com/navikt/utenlandsadresser/assets/6861919/b920291d-ce15-4016-b828-47a5e50f7264)

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

## Sporingslogg retention policy

Hver gang vi utleverer en postadresse til en konsument, lagres det en sporingslogg i databasen. Sporingsloggen er ment for å kunne brukes til å gi innsyn til privatpersoner på hvilke adresser som er delt om de ber om det.

Sporingslogger eldre enn 10 år skal slettes. Dette gjøres ved å kjøre en naisjob som kjører i starten av hver måned. Naisjobben er definert i [sporingslogg-cleanup-job.yaml](sporingslogg-cleanup/.nais/nais.yaml).

Jobben gjør et  HTTP-kall mot [appen](app) som inneholder hvor gamle sporingsloggene må være før de slettes. Implementasjonsdetaljer finnes i [sporingslogg-cleanup](sporingslogg-cleanup).

## Hent utenlandsadresser (POC)

Proof of concept for å hente utenlandsadresser fra Skatteetaten og bruke PDL mottak for å oppdatere PDL med utenlandsadresser.

## Beslutningslogg

- For å ikke duplisere logikk valgte vi å gjennbruke logikken til Team Dokumenthåndtering for å velge postadresse. Dette gjøres gjennom Registeroppslagt-APIet. Det andre alternativet vi vurderte var å koble oss direkte på PDLs GrapQL-grensesnitt.
- Adresser vi har delt med Skatteetaten lages in en SQL-database som sporingslogg. Sporingsloggen kan brukes om vi trenger å gi ut innsyn til hvilke adresser som er delt med Skatteetaten.

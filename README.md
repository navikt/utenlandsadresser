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

### Stoppede abonnementer

Hendelser som lå på feeden før et abonnement ble stoppet, leveres fortsatt, men uten data. Da kan konsumenten gå videre til neste løpenummer, uten at vi deler adresser eller id-er for personer de ikke lenger abonnerer på.
Hendelser om adressebeskyttelse leveres alltid, så konsumenten sletter adressen.

### Utenlandsk id

Utenlandsk id har et eget abonnement og en egen feed under `/api/v1/utenlandskid`, med eget Maskinporten-scope (`nav:utenlandsadresser:utenlandskid.read`).
Når abonnementet startes, slår tjenesten opp i PDL. Har personen utenlandsk id, legges det en hendelse på feeden.
Feeden lagrer bare identitetsnummer. Gjeldende utenlandske id-er hentes fra PDL når feeden leses, og listen kan være tom om id-en er opphørt.
PDL har foreløpig ingen Kafka-hendelse for utenlandsk id, så nye eller endrede id-er etter oppstart fanges ikke opp.

Utenlandsk id er skjult bak Unleash-togglen `utenlandsadresser.utenlandsk-id`. Når togglen er av, svarer alle endepunkter under `/api/v1/utenlandskid` 404, også uten token. Togglen sjekkes ved hvert kall, så den kan slås av og på uten deploy. Kan ikke appen nå Unleash når den starter, er togglen av. Lokalt er togglen alltid på.
Togglen styres i [Unleash](https://utenlandsadresser-unleash-web.iap.nav.cloud.nais.io). Når utenlandsk id er i prod, skal togglen og `FeatureToggleGate` fjernes.

## Sporingslogg retention policy

Hver gang vi utleverer en postadresse eller utenlandsk id til en konsument, lagres det en sporingslogg i databasen. Sporingsloggen er ment for å kunne brukes til å gi innsyn til privatpersoner på hvilke adresser som er delt om de ber om det.

Sporingslogger eldre enn 10 år skal slettes. Dette gjøres ved å kjøre en naisjob som kjører i starten av hver måned. Naisjobben er definert i [sporingslogg-cleanup-job.yaml](sporingslogg-cleanup/.nais/nais.yaml).

Jobben gjør et  HTTP-kall mot [appen](app) som inneholder hvor gamle sporingsloggene må være før de slettes. Implementasjonsdetaljer finnes i [sporingslogg-cleanup](sporingslogg-cleanup).

## Hent utenlandsadresser (POC)

Proof of concept for å hente utenlandsadresser fra Skatteetaten og bruke PDL mottak for å oppdatere PDL med utenlandsadresser.

## Beslutningslogg

- For å ikke duplisere logikk valgte vi å gjennbruke logikken til Team Dokumenthåndtering for å velge postadresse. Dette gjøres gjennom Registeroppslagt-APIet. Det andre alternativet vi vurderte var å koble oss direkte på PDLs GrapQL-grensesnitt.
- Adresser vi har delt med Skatteetaten lages in en SQL-database som sporingslogg. Sporingsloggen kan brukes om vi trenger å gi ut innsyn til hvilke adresser som er delt med Skatteetaten.
- `app` følger ports and adapters. Forretningsreglene ligger i `application` og bruker bare `domain` og porter. Ktor, Kafka, Postgres og eksterne API-er ligger i `adapter`. `ArchitectureTest` stopper avhengigheter i feil retning. Delt kode mellom modulene ligger i `felles`, så ingen modul avhenger av `app`.

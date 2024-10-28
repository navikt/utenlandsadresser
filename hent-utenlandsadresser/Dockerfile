FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

COPY build/install/hent-utenlandsadresser/lib/ lib/

ENTRYPOINT ["java"]

CMD ["-cp", "/hent-utenlandsadresser/lib/*", "no.nav.utenlandsadresser.hent.utenlandsadresser.MainKt"]
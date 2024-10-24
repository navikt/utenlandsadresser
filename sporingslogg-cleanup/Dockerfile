FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

COPY build/install/sporingslogg-cleanup/lib/ lib/

ENTRYPOINT ["java"]

CMD ["-cp", "/sporingslogg-cleanup/lib/*", "no.nav.utenlandsadresser.sporingslogg.cleanup.MainKt"]

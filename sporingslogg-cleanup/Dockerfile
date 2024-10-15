FROM gcr.io/distroless/java17-debian12:nonroot

WORKDIR /app

COPY build/install/sporingslogg-cleanup/lib/ lib/

CMD ["java", "-cp", "/sporingslogg-cleanup/lib/*", "no.nav.utenlandsadresser.sporingslogg.cleanup.MainKt"]

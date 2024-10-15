FROM gcr.io/distroless/java17-debian12:nonroot

WORKDIR /app

COPY build/install/app/lib/ lib/

CMD ["java", "-cp", "/app/lib/*", "no.nav.utenlandsadresser.ApplicationKt"]

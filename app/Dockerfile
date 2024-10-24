FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

COPY build/install/app/lib/ lib/

ENTRYPOINT ["java"]

CMD ["-cp", "/app/lib/*", "no.nav.utenlandsadresser.ApplicationKt"]

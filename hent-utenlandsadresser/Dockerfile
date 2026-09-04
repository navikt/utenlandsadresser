FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /app

COPY build/install/hent-utenlandsadresser/lib/ lib/

ENTRYPOINT ["java"]

CMD ["-cp", "/app/lib/*", "no.nav.utenlandsadresser.hent.utenlandsadresser.MainKt"]
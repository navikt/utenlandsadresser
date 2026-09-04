FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /app

COPY build/install/app/lib/ lib/

ENTRYPOINT ["java"]

CMD ["-Dorg.apache.avro.SERIALIZABLE_PACKAGES=no.nav.person.pdl.leesah", "-cp", "/app/lib/*", "no.nav.utenlandsadresser.ApplicationKt"]

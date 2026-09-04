FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /app

COPY build/install/app/lib/ lib/

ENTRYPOINT ["java"]

ENV AVRO_SERIALIZABLE_PACKAGES="no.nav.person.pdl.leesah"

CMD ["-Dorg.apache.avro.SERIALIZABLE_PACKAGES=\"$AVRO_SERIALIZABLE_PACKAGES\"", "-cp", "/app/lib/*", "no.nav.utenlandsadresser.ApplicationKt"]

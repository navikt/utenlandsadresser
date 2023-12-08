FROM gcr.io/distroless/java17-debian11:nonroot

WORKDIR /app

COPY build/libs/utenlandsadresser-all.jar .

CMD ["/app/utenlandsadresser-all.jar"]

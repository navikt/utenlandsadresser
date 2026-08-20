package no.nav.utenlandsadresser.infrastructure.persistence

fun r2dbcUrl(
    driver: String,
    user: String,
    password: String,
    host: String,
    port: String,
    path: String,
): String = "r2dbc:$driver://$user:$password@$host:$port/$path"

package no.nav.utenlandsadresser.clients.graphql

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class GraphQlQuery(val value: String)

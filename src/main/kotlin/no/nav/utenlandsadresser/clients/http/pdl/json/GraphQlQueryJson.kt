package no.nav.utenlandsadresser.clients.http.pdl.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery

@Serializable
data class GraphQlQueryJson(
    val query: GraphQlQuery,
)
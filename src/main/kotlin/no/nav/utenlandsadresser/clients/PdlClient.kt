package no.nav.utenlandsadresser.clients

import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import kotlin.reflect.KClass

interface PdlClient {
    suspend fun <T: Any> executeQuery(query: GraphQlQuery, responseType: KClass<T>): T
}



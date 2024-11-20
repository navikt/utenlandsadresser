package no.nav.utenlandsadresser.sporingslogg.cleanup

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.client.request.delete
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.infrastructure.client.http.createHttpClient
import no.nav.utenlandsadresser.sporingslogg.cleanup.config.SporingsloggCleanupConfig
import no.nav.utenlandsadresser.util.years
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

suspend fun main() {
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    configureLogging(appEnv)

    val logger = LoggerFactory.getLogger("SporingsloggCleanup")

    val resourceFiles =
        listOfNotNull(
            when (appEnv) {
                AppEnv.LOCAL -> "/sporingslogg-cleanup-local.conf"
                AppEnv.DEV_GCP,
                AppEnv.PROD_GCP,
                -> null
            },
            "/sporingslogg-cleanup.conf",
        )

    val config: SporingsloggCleanupConfig = ConfigLoader().loadConfigOrThrow(resourceFiles)

    val client = createHttpClient()

    val deleteSporingsloggerOlderThan = 10.years.toIsoString()

    val response =
        client.delete("${config.utenlandsadresser.baseUrl}/internal/sporingslogg?olderThan=$deleteSporingsloggerOlderThan")

    if (response.status.isSuccess()) {
        logger.info("Successfully deleted sporingslogg older than $deleteSporingsloggerOlderThan")
    } else {
        logger.error("Failed to delete sporingslogg. Response status: ${response.status} with body: ${response.bodyAsText()}")
        exitProcess(1)
    }
}

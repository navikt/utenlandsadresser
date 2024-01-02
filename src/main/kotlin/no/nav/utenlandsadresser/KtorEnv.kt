package no.nav.utenlandsadresser

enum class KtorEnv {
    LOCAL, DEV_GCP, PROD_GCP;

    companion object {
        fun getFromEnvVariable(name: String): KtorEnv {
            val envVal = System.getenv(name)
            return when (envVal) {
                "dev-gcp" -> DEV_GCP
                "prod-gcp" -> PROD_GCP
                else -> LOCAL
            }
        }
    }
}
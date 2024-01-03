package no.nav.utenlandsadresser

enum class KtorEnv {
    LOCAL, DEV_GCP, PROD_GCP;

    companion object {
        fun getFromEnvVariable(name: String): KtorEnv {
            val envVal = System.getenv(name)
            return when (envVal) {
                "local" -> LOCAL
                "dev-gcp" -> DEV_GCP
                else -> PROD_GCP
            }
        }
    }
}
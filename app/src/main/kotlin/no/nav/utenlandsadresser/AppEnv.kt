package no.nav.utenlandsadresser

enum class AppEnv {
    LOCAL,
    DEV_GCP,
    PROD_GCP,
    ;

    companion object {
        fun getFromEnvVariable(name: String): AppEnv =
            when (System.getenv(name)) {
                "dev-gcp" -> DEV_GCP
                "prod-gcp" -> PROD_GCP
                "local" -> LOCAL
                else -> LOCAL
            }
    }
}

package no.nav.utenlandsadresser

enum class KtorEnv {
    LOCAL, DEV_GCP, PROD_GCP;

    companion object {
        fun fromEnvVariable(env: String?): KtorEnv {
            return when (env) {
                "dev-gcp" -> DEV_GCP
                "prod-gcp" -> PROD_GCP
                else -> LOCAL
            }
        }
    }
}
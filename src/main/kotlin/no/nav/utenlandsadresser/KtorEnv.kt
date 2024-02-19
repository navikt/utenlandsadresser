package no.nav.utenlandsadresser

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import org.slf4j.Logger
import kotlin.reflect.KType
import kotlin.reflect.typeOf

enum class KtorEnv {
    LOCAL, DEV_GCP, PROD_GCP;

    companion object {
        fun getFromEnvVariable(name: String): KtorEnv = when (System.getenv(name)) {
            "dev-gcp" -> DEV_GCP
            "prod-gcp" -> PROD_GCP
            "local" -> LOCAL
            else -> LOCAL
        }
    }
}

object KtorEnvDecoder : Decoder<KtorEnv> {
    override fun decode(node: Node, type: KType, context: DecoderContext): ConfigResult<KtorEnv> =
        when (node) {
            is StringNode -> when (node.value) {
                "local" -> KtorEnv.LOCAL.valid()
                "dev-gcp" -> KtorEnv.DEV_GCP.valid()
                "prod-gcp" -> KtorEnv.PROD_GCP.valid()
                else -> ConfigFailure.DecodeError(node, typeOf<KtorEnv>()).invalid()
            }
            else -> ConfigFailure.DecodeError(node, typeOf<KtorEnv>()).invalid()
        }

    override fun supports(type: KType): Boolean = type == typeOf<KtorEnv>()
}

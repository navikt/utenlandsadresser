package no.nav.utenlandsadresser.clients.http.maskinporten.json

import kotlinx.serialization.Serializable
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateCrtKeySpec
import java.util.*

@Serializable
data class RsaPrivateKey(
    val p: String,
    val kty: String,
    val q: String,
    val d: String,
    val e: String,
    val use: String,
    val kid: String,
    val qi: String,
    val dp: String,
    val alg: String,
    val dq: String,
    val n: String
) {
    fun toRSAPrivateKey(): RSAPrivateKey {
        val modulus = n.decodeBase64ToBigInteger()
        val publicExp = e.decodeBase64ToBigInteger()
        val privateExp = d.decodeBase64ToBigInteger()
        val primeP = p.decodeBase64ToBigInteger()
        val primeQ = q.decodeBase64ToBigInteger()
        val primeExpP = dp.decodeBase64ToBigInteger()
        val primeExpQ = dq.decodeBase64ToBigInteger()
        val crtCoeff = qi.decodeBase64ToBigInteger()

        val keySpec = RSAPrivateCrtKeySpec(
            modulus, publicExp, privateExp, primeP, primeQ, primeExpP, primeExpQ, crtCoeff
        )

        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun String.decodeBase64ToBigInteger(): BigInteger = BigInteger(1, Base64.getUrlDecoder().decode(this))
}
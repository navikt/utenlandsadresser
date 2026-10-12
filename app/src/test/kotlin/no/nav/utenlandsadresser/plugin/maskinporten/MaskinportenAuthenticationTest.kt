package no.nav.utenlandsadresser.plugin.maskinporten

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.every
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date

private fun generateRsaKeyPair(): KeyPair =
    KeyPairGenerator
        .getInstance("RSA")
        .apply { initialize(2048) }
        .genKeyPair()

private fun rsaAlgorithm(keyPair: KeyPair): Algorithm =
    Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey)

class MaskinportenAuthenticationTest :
    WordSpec({
        val scope = Scope("test-scope")
        val organisasjonsnummer = Organisasjonsnummer("889640782")
        val issuer = Issuer("https://maskinporten.no")
        val jwkProvider = mockk<JwkProvider>()
        val keyPair = generateRsaKeyPair()
        val otherKeyPair = generateRsaKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val client =
            specWideTestApplication {
                application {
                    configureMaskinportenAuthentication(
                        configurationName = "test-maskinporten",
                        issuer = issuer,
                        requiredScopes = setOf(scope),
                        jwkProvider = jwkProvider,
                        jwtValidationBlock = validateOrganisasjonsnummer(listOf(organisasjonsnummer.value)),
                    )

                    routing {
                        authenticate("test-maskinporten") {
                            route("/test") {
                                get("/hello") {
                                    call.attributes[OrganisasjonsnummerKey] shouldBe organisasjonsnummer.value

                                    call.respond("Hello")
                                }
                            }
                        }
                    }
                }
            }.client

        beforeTest {
            every { jwkProvider.get(any()) } returns
                Jwk(
                    "keyId",
                    "RSA",
                    "RS256",
                    "",
                    emptyList(),
                    "",
                    emptyList(),
                    "",
                    mapOf(
                        "n" to Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray()),
                        "e" to Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray()),
                    ),
                )
        }

        fun token(
            tokenIssuer: String = issuer.value,
            tokenScope: String? = scope.value,
            consumerId: String? = "0192:889640782",
            expiresAt: Date = Date(System.currentTimeMillis() + 60_000),
            signingKeyPair: KeyPair = keyPair,
            algorithm: Algorithm = rsaAlgorithm(signingKeyPair),
        ): String {
            val token = JWT.create().withIssuer(tokenIssuer).withExpiresAt(expiresAt)
            tokenScope?.let { token.withClaim("scope", it) }
            consumerId?.let { token.withClaim("consumer", mapOf("ID" to it)) }
            return token.sign(algorithm)
        }

        "GET /test/hello" should {
            "respond with UNAUTHORIZED when no jwt is provided" {
                val response = client.get("/test/hello")

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when no scope is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(tokenScope = null))
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when wrong scope is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(tokenScope = "wrong-scope"))
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when no consumer.ID claim is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(consumerId = null))
                    }

                response.status.value shouldBe 401
            }

            "respond with OK when correct jwt is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token())
                    }

                response.status.value shouldBe 200
            }

            "respond with UNAUTHORIZED when an unregistered consumer is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(consumerId = "0192:974761076"))
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when an incorrect issuer is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(tokenIssuer = "https://evil.example"))
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when token is signed with an untrusted key" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(signingKeyPair = otherKeyPair))
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when token is expired" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(expiresAt = Date(System.currentTimeMillis() - 60_000)))
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when token uses the none algorithm" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(algorithm = Algorithm.none()))
                    }

                response.status.value shouldBe 401
            }

            "respond with OK when multiple scopes include the required scope" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(token(tokenScope = "other-scope test-scope"))
                    }

                response.status.value shouldBe 200
            }
        }
    })

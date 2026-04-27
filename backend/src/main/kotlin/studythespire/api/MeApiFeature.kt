package studythespire.api

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.exception.ExpiredJwt
import kairo.rest.exception.JwtVerificationFailed
import kairo.rest.exception.NoJwt
import kairo.rest.route
import java.net.URI
import java.security.interfaces.RSAPublicKey

internal class MeApiFeature(
  clerkConfig: ClerkConfig,
) : Feature(), HasRouting {
  override val name: String = "MeApi"

  private val issuer = clerkConfig.issuer
  private val jwkProvider = JwkProviderBuilder(URI.create(clerkConfig.jwksUrl).toURL()).build()

  override fun Application.routing() {
    routing {
      route(MeApi.Get::class) {
        // Kairo's verify() can't be used here: Route.route() doesn't wrap in authenticate(),
        // so call.principal<BearerTokenCredential>() is always null. Verify the JWT manually
        // and stash the decoded payload on the call attributes for the handler to read.
        auth {
          val authHeader = call.request.headers["Authorization"] ?: throw NoJwt()
          val token = authHeader.removePrefix("Bearer ").trim()
            .takeIf { it.isNotEmpty() } ?: throw NoJwt()
          val decoded = try {
            val unverified = JWT.decode(token)
            val jwk = jwkProvider.get(unverified.keyId)
            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
            JWT.require(algorithm)
              .withIssuer(issuer)
              .acceptLeeway(5)
              .build()
              .verify(token)
          } catch (e: TokenExpiredException) {
            throw ExpiredJwt(e)
          } catch (e: JWTVerificationException) {
            throw JwtVerificationFailed(e)
          } catch (e: Exception) {
            throw JwtVerificationFailed(e)
          }
          call.attributes.put(DecodedJwtKey, decoded)
        }
        handle {
          val decoded = call.attributes[DecodedJwtKey]
          MeRep(
            userId = decoded.subject,
            email = decoded.getClaim("email").asString(),
          )
        }
      }
    }
  }

  companion object {
    private val DecodedJwtKey = AttributeKey<DecodedJWT>("decodedClerkJwt")
  }
}

@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.server.routing.RoutingCall
import io.ktor.util.AttributeKey
import kairo.rest.exception.ExpiredJwt
import kairo.rest.exception.JwtVerificationFailed
import kairo.rest.exception.NoJwt
import studythespire.api.ClerkConfig
import studythespire.api.users.User
import studythespire.api.users.UserStore
import java.net.URI
import java.security.interfaces.RSAPublicKey

/**
 * Verifies a Clerk-issued JWT on the incoming request, looks up (or bootstraps)
 * the matching internal user row, and stashes it on the call attributes.
 *
 * Kairo's built-in `verify()` can't be used here: `Route.route()` does not wrap routes
 * in Ktor's `authenticate()`, so `call.principal<BearerTokenCredential>()` always returns null.
 * We do JWT verification manually with Auth0 java-jwt instead.
 */
internal class ClerkAuthHelper(
  clerkConfig: ClerkConfig,
  private val userStore: UserStore,
) {
  private val issuer = clerkConfig.issuer
  private val jwkProvider = JwkProviderBuilder(URI.create(clerkConfig.jwksUrl).toURL()).build()

  suspend fun authenticate(call: RoutingCall): User {
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
    val user = userStore.getOrCreate(
      clerkUserId = decoded.subject,
      email = decoded.getClaim("email").asString(),
    )
    call.attributes.put(UserKey, user)
    return user
  }

  companion object {
    val UserKey: AttributeKey<User> = AttributeKey("studythespire.user")
  }
}

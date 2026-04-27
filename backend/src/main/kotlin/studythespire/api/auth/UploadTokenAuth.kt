package studythespire.api.auth

import io.ktor.server.routing.RoutingCall
import io.ktor.util.AttributeKey
import kairo.rest.exception.JwtVerificationFailed
import kairo.rest.exception.NoJwt
import studythespire.api.tokens.UploadToken
import studythespire.api.tokens.UploadTokenSecret
import studythespire.api.tokens.UploadTokenStore

/**
 * Verifies an `Authorization: Bearer stsa_live_<secret>` header against the upload_tokens table.
 *
 * Reuses Kairo's existing JWT exception types (`NoJwt`, `JwtVerificationFailed`) so the
 * status-pages plugin maps both upload-token and Clerk-JWT failures to 401 without any new wiring.
 * Proper exception types can come in M17 if needed.
 */
internal class UploadTokenAuth(
  private val store: UploadTokenStore,
) {
  suspend fun authenticate(call: RoutingCall): UploadToken {
    val authHeader = call.request.headers["Authorization"] ?: throw NoJwt()
    val raw = authHeader.removePrefix("Bearer ").trim()
    if (!raw.startsWith("stsa_live_")) throw NoJwt()
    val token = store.findByHash(UploadTokenSecret.hash(raw))
      ?: throw JwtVerificationFailed(IllegalArgumentException("Unknown or revoked upload token"))
    runCatching { store.touchLastUsed(token.id) }  // best-effort, never block auth
    call.attributes.put(UploadTokenKey, token)
    return token
  }

  companion object {
    val UploadTokenKey: AttributeKey<UploadToken> = AttributeKey("studythespire.uploadToken")
  }
}

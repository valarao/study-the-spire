package studythespire.api.tokens

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Token format: `stsa_live_<32-byte-base64url>` (~53 chars).
 * The DB stores the SHA-256 hash; the raw secret is shown to the user only once at create time.
 */
internal object UploadTokenSecret {
  private const val PREFIX = "stsa_live_"
  private const val PREFIX_DISPLAY_CHARS = 14
  private val random = SecureRandom()

  fun generate(): String {
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  fun hash(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun prefix(token: String): String =
    token.take(PREFIX_DISPLAY_CHARS)
}

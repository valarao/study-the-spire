package studythespire.api.auth

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.bearer
import kairo.rest.auth.AuthConfig
import kairo.rest.auth.AuthReceiver
import kairo.rest.auth.VerifierConfig
import kairo.rest.auth.public
import studythespire.api.ClerkConfig
import kotlin.time.Duration.Companion.seconds

internal class ClerkAuth(config: ClerkConfig) : AuthConfig() {
  val verifierConfig: VerifierConfig = VerifierConfig(
    jwkUrl = config.jwksUrl,
    issuer = config.issuer,
    leeway = 5.seconds,
  )

  override fun AuthenticationConfig.configure() {
    bearer {
      authenticate { credential -> credential }
    }
  }

  // Routes that don't define their own auth{} block are public by default.
  // Endpoints requiring auth must explicitly call verify(verifierConfig).
  override suspend fun AuthReceiver<*>.default() {
    public()
  }
}

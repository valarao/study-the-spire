package studythespire.api

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import studythespire.api.auth.ClerkAuthHelper

internal class MeApiFeature(
  private val auth: ClerkAuthHelper,
) : Feature(), HasRouting {
  override val name: String = "MeApi"

  override fun Application.routing() {
    routing {
      route(MeApi.Get::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          MeRep(userId = user.clerkUserId, email = user.email)
        }
      }
    }
  }
}

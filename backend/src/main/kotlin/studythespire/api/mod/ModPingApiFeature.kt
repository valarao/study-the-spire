package studythespire.api.mod

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import studythespire.api.auth.UploadTokenAuth

internal class ModPingApiFeature(
  private val auth: UploadTokenAuth,
  private val serverVersion: String,
) : Feature(), HasRouting {
  override val name: String = "ModPingApi"

  override fun Application.routing() {
    routing {
      route(ModPingApi.Post::class) {
        auth { auth.authenticate(call) }
        handle {
          val token = call.attributes[UploadTokenAuth.UploadTokenKey]
          ModPingRep(
            ok = true,
            tokenName = token.name,
            serverVersion = serverVersion,
          )
        }
      }
    }
  }
}

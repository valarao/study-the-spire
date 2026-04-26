package studythespire.api

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route

/** Registers public HTTP routes for Milestone 2 (expands in later milestones). */
internal class HelloApiFeature : Feature(), HasRouting {
  override val name: String = "HelloApi"

  override fun Application.routing() {
    routing {
      route(HelloApi.Get::class) {
        handle {
          HelloRep(message = "the spire awaits")
        }
      }
    }
  }
}

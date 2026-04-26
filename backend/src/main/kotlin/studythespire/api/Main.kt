package studythespire.api

import kairo.application.kairo
import kairo.config.loadConfig
import kairo.healthCheck.HealthCheckFeature
import kairo.rest.RestFeature
import kairo.server.Server

fun main() =
  kairo {
    val config = loadConfig<AppConfig>()

    val server =
      Server(
        name = "Study the Spire API",
        features =
          listOf(
            HealthCheckFeature(),
            RestFeature(
              config = config.rest,
              authConfig = null,
            ),
            HelloApiFeature(),
          ),
      )

    server.startAndWait(
      release = { server.stop() },
    )
  }

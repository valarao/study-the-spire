package studythespire.api

import kairo.application.kairo
import kairo.config.loadConfig
import kairo.dependencyInjection.DependencyInjectionFeature
import kairo.healthCheck.HealthCheckFeature
import kairo.rest.RestFeature
import kairo.server.Server
import kairo.sql.SqlFeature
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.koin.dsl.koinApplication
import studythespire.api.auth.ClerkAuth
import studythespire.api.health.DbPingFeature

fun main() =
  kairo {
    val config = loadConfig<AppConfig>()
    val koinApplication = koinApplication()
    val clerkAuth = ClerkAuth(config.clerk)

    val server =
      Server(
        name = "Study the Spire API",
        features =
          listOf(
            DependencyInjectionFeature(koinApplication),
            HealthCheckFeature(),
            RestFeature(
              config = config.rest,
              authConfig = clerkAuth,
            ),
            SqlFeature(
              config = config.sql,
              configureDatabase = {
                explicitDialect = PostgreSQLDialect()
              },
            ),
            HelloApiFeature(),
            DbPingFeature(koinApplication.koin),
            MeApiFeature(config.clerk),
          ),
      )

    server.startAndWait(
      release = { server.stop() },
    )
  }

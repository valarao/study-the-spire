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
import studythespire.api.auth.ClerkAuthHelper
import studythespire.api.auth.UploadTokenAuth
import studythespire.api.db.Migrator
import studythespire.api.health.DbPingFeature
import studythespire.api.mod.ModPingApiFeature
import studythespire.api.tokens.UploadTokenStore
import studythespire.api.tokens.UploadTokensApiFeature
import studythespire.api.users.UserStore

fun main() =
  kairo {
    val config = loadConfig<AppConfig>()
    Migrator.migrate()
    val koinApplication = koinApplication()
    val koin = koinApplication.koin
    val clerkAuth = ClerkAuth(config.clerk)
    val userStore = UserStore(koin)
    val uploadTokenStore = UploadTokenStore(koin)
    val clerkAuthHelper = ClerkAuthHelper(config.clerk, userStore)
    val uploadTokenAuth = UploadTokenAuth(uploadTokenStore)

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
            DbPingFeature(koin),
            MeApiFeature(clerkAuthHelper),
            UploadTokensApiFeature(clerkAuthHelper, uploadTokenStore),
            ModPingApiFeature(uploadTokenAuth, serverVersion = "0.1.0"),
          ),
      )

    server.startAndWait(
      release = { server.stop() },
    )
  }

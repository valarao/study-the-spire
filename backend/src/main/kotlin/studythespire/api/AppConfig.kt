package studythespire.api

import kairo.rest.RestFeatureConfig
import kairo.sql.SqlFeatureConfig

data class ClerkConfig(
  val jwksUrl: String,
  val issuer: String,
)

data class AppConfig(
  val rest: RestFeatureConfig,
  val sql: SqlFeatureConfig,
  val clerk: ClerkConfig,
)

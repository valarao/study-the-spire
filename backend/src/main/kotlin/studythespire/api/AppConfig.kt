package studythespire.api

import kairo.rest.RestFeatureConfig
import kairo.sql.SqlFeatureConfig

data class AppConfig(
  val rest: RestFeatureConfig,
  val sql: SqlFeatureConfig,
)

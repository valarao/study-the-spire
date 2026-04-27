package studythespire.api.db

import org.flywaydb.core.Flyway

/**
 * Runs Flyway SQL migrations on startup, before the Ktor server binds.
 *
 * Reads connection details from env vars so prod and local share one code path.
 * If `INSTANCE_CONNECTION_NAME` is set we use the Cloud SQL JDBC socket factory;
 * otherwise we connect to a plain `host:port`. Local defaults match
 * `infra/local/docker-compose.yml` so `./gradlew run` works without env setup.
 */
internal object Migrator {
  fun migrate() {
    val dbName = env("DB_NAME") ?: "study_the_spire"
    val dbUser = env("DB_USER") ?: "postgres"
    val dbPassword = env("DB_PASSWORD") ?: "postgres"
    val instance = env("INSTANCE_CONNECTION_NAME")

    val jdbcUrl = if (instance != null) {
      "jdbc:postgresql:///$dbName" +
        "?cloudSqlInstance=$instance" +
        "&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    } else {
      val host = env("DB_HOST") ?: "localhost"
      val port = env("DB_PORT") ?: "5432"
      "jdbc:postgresql://$host:$port/$dbName"
    }

    Flyway.configure()
      .dataSource(jdbcUrl, dbUser, dbPassword)
      .locations("classpath:db/migration")
      .load()
      .migrate()
  }

  private fun env(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
}

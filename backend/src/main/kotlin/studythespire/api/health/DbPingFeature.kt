package studythespire.api.health

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import java.time.Instant
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.Koin

/**
 * Round-trips a trivial query to Postgres so callers can confirm Cloud Run <-> Cloud SQL is healthy.
 *
 * `databaseTime` is `Instant.now()` reported only after the DB round-trip succeeds; an unreachable
 * DB causes [suspendTransaction] to throw before we ever reach the response builder.
 */
internal class DbPingFeature(
  private val koin: Koin,
) : Feature(),
  HasRouting {
  override val name: String = "DbPing"

  private val database: R2dbcDatabase get() = koin.get()

  override fun Application.routing() {
    routing {
      route(DbPingApi.Get::class) {
        handle {
          suspendTransaction(db = database) {
            exec("SELECT 1")
          }
          DbPingRep(ok = true, databaseTime = Instant.now())
        }
      }
    }
  }
}

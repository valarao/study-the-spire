package studythespire.api.health

import java.time.Instant
import kairo.rest.Rest
import kairo.rest.RestEndpoint

internal data class DbPingRep(
  val ok: Boolean,
  val databaseTime: Instant,
)

internal object DbPingApi {
  @Rest("GET", "/db/ping")
  @Rest.Accept("application/json")
  data object Get : RestEndpoint<Unit, DbPingRep>()
}

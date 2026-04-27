package studythespire.api

import kairo.rest.Rest
import kairo.rest.RestEndpoint

internal data class MeRep(
  val userId: String,
  val email: String?,
)

internal object MeApi {
  @Rest("GET", "/me")
  @Rest.Accept("application/json")
  data object Get : RestEndpoint<Unit, MeRep>()
}

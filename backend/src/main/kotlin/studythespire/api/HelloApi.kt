package studythespire.api

import kairo.rest.Rest
import kairo.rest.RestEndpoint

internal data class HelloRep(
  val message: String,
)

internal object HelloApi {
  @Rest("GET", "/hello")
  @Rest.Accept("application/json")
  data object Get : RestEndpoint<Unit, HelloRep>()
}

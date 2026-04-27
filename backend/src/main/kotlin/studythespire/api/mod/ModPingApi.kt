package studythespire.api.mod

import kairo.rest.Rest
import kairo.rest.RestEndpoint

internal data class ModPingReq(
  val modVersion: String,
  val gameVersion: String,
)

internal data class ModPingRep(
  val ok: Boolean,
  val tokenName: String,
  val serverVersion: String,
)

internal object ModPingApi {
  @Rest("POST", "/mod/ping")
  @Rest.ContentType("application/json")
  @Rest.Accept("application/json")
  data class Post(
    override val body: ModPingReq,
  ) : RestEndpoint<ModPingReq, ModPingRep>()
}

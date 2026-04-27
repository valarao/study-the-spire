package studythespire.api.tokens

import kairo.rest.Rest
import kairo.rest.RestEndpoint

internal data class UploadTokenRep(
  val id: String,
  val name: String,
  val tokenPrefix: String,
  val createdAt: String,
  val lastUsedAt: String?,
)

internal data class UploadTokensListRep(val tokens: List<UploadTokenRep>)

internal data class CreateUploadTokenReq(val name: String)

internal data class CreateUploadTokenRep(
  val token: UploadTokenRep,
  val secret: String,
)

internal data class DeleteUploadTokenRep(val ok: Boolean)

internal object UploadTokensApi {
  @Rest("GET", "/upload-tokens")
  @Rest.Accept("application/json")
  data object List : RestEndpoint<Unit, UploadTokensListRep>()

  @Rest("POST", "/upload-tokens")
  @Rest.ContentType("application/json")
  @Rest.Accept("application/json")
  data class Create(
    override val body: CreateUploadTokenReq,
  ) : RestEndpoint<CreateUploadTokenReq, CreateUploadTokenRep>()

  @Rest("DELETE", "/upload-tokens/:tokenId")
  @Rest.Accept("application/json")
  data class Delete(
    @PathParam val tokenId: String,
  ) : RestEndpoint<Unit, DeleteUploadTokenRep>()
}

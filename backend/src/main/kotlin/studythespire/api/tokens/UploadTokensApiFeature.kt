@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.tokens

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import studythespire.api.auth.ClerkAuthHelper
import kotlin.uuid.Uuid

internal class UploadTokensApiFeature(
  private val auth: ClerkAuthHelper,
  private val store: UploadTokenStore,
) : Feature(), HasRouting {
  override val name: String = "UploadTokensApi"

  override fun Application.routing() {
    routing {
      route(UploadTokensApi.List::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          val tokens = store.listForUser(user.id).map { it.toRep() }
          UploadTokensListRep(tokens = tokens)
        }
      }

      route(UploadTokensApi.Create::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          val (token, secret) = store.create(user.id, endpoint.body.name)
          CreateUploadTokenRep(token = token.toRep(), secret = secret)
        }
      }

      route(UploadTokensApi.Delete::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          val parsedId = runCatching { UploadTokenId(Uuid.parse(endpoint.tokenId)) }.getOrNull()
          val ok = parsedId != null && store.revoke(parsedId, user.id)
          DeleteUploadTokenRep(ok = ok)
        }
      }
    }
  }
}

private fun UploadToken.toRep(): UploadTokenRep =
  UploadTokenRep(
    id = id.toString(),
    name = name,
    tokenPrefix = tokenPrefix,
    createdAt = createdAt.toString(),
    lastUsedAt = lastUsedAt?.toString(),
  )

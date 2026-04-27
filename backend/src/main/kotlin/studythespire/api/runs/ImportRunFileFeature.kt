package studythespire.api.runs

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import studythespire.api.auth.UploadTokenAuth
import java.security.MessageDigest

internal class ImportRunFileFeature(
  private val auth: UploadTokenAuth,
  private val runs: RunStore,
) : Feature(), HasRouting {
  override val name: String = "ImportRunFile"

  override fun Application.routing() {
    routing {
      route(ImportsApi.Post::class) {
        auth { auth.authenticate(call) }
        handle {
          val token = call.attributes[UploadTokenAuth.UploadTokenKey]
          val rawJson = endpoint.body
          val sha = sha256Hex(rawJson)
          val fileName = call.request.headers["X-Run-File-Name"]
          val result = runs.importRun(
            userId = token.userId,
            sha256 = sha,
            fileName = fileName,
            rawJson = rawJson,
          )
          when (result) {
            is ImportResult.Inserted -> ImportRunFileRep(imported = true, runId = result.runId.toString())
            is ImportResult.Duplicate -> ImportRunFileRep(imported = false, runId = result.runId.toString())
          }
        }
      }
    }
  }
}

private fun sha256Hex(s: String): String {
  val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
  return digest.joinToString("") { "%02x".format(it) }
}

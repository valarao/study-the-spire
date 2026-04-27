@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.runs

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import studythespire.api.auth.ClerkAuthHelper
import kotlin.uuid.Uuid

internal class RunsApiFeature(
  private val auth: ClerkAuthHelper,
  private val runs: RunStore,
) : Feature(), HasRouting {
  override val name: String = "RunsApi"

  override fun Application.routing() {
    routing {
      route(RunsApi.List::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          RunsListRep(runs = runs.listForUser(user.id).map { it.toRep() })
        }
      }

      route(RunsApi.Get::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          val parsed = runCatching { Uuid.parse(endpoint.runId) }.getOrNull()
            ?: error("invalid runId")
          val detail = runs.findById(RunId(parsed), user.id)
            ?: error("run not found")
          RunDetailRep(
            run = detail.run.toRep(),
            rawJson = detail.rawJson,
            fileName = detail.fileName,
          )
        }
      }
    }
  }
}

internal fun Run.toRep(): RunRep = RunRep(
  id = id.toString(),
  status = status.value,
  characterClass = characterClass,
  ascension = ascension,
  seed = seed,
  buildId = buildId,
  gameMode = gameMode,
  platformType = platformType,
  startTime = startTime.toString(),
  runTimeSecs = runTimeSecs,
  killedByEncounter = killedByEncounter,
  killedByEvent = killedByEvent,
  schemaVersion = schemaVersion,
  acts = acts,
)

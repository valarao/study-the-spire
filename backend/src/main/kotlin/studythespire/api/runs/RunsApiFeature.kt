@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)

package studythespire.api.runs

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kairo.feature.Feature
import kairo.rest.HasRouting
import kairo.rest.route
import studythespire.api.auth.ClerkAuthHelper
import kotlin.time.Instant
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
          val limit = endpoint.limit?.coerceIn(1, 100) ?: 25
          val cursor = endpoint.cursor?.let { RunCursor.decode(it) }
          val filter = RunFilter(
            character = endpoint.character?.takeIf { it.isNotBlank() },
            ascension = endpoint.ascension,
            status = endpoint.status?.let { s ->
              RunStatus.entries.firstOrNull { it.value == s }
            },
            from = endpoint.from?.let { runCatching { Instant.parse(it) }.getOrNull() },
            to = endpoint.to?.let { runCatching { Instant.parse(it) }.getOrNull() },
          )
          val page = runs.listForUser(user.id, filter, cursor, limit)
          RunsListRep(
            runs = page.runs.map { it.toRep() },
            nextCursor = page.nextCursor,
          )
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

      route(StatsApi.Summary::class) {
        auth { auth.authenticate(call) }
        handle {
          val user = call.attributes[ClerkAuthHelper.UserKey]
          val agg = runs.summaryForUser(user.id)
          StatsSummaryRep(
            totalRuns = agg.totalRuns,
            wins = agg.wins,
            defeats = agg.defeats,
            abandoned = agg.abandoned,
            winRate = if (agg.totalRuns == 0) null else agg.wins.toDouble() / agg.totalRuns,
            avgRunTimeSecs = if (agg.totalRuns == 0) null else (agg.totalRunTimeSecs / agg.totalRuns).toInt(),
            byCharacter = agg.byCharacter.map { (c, r, w) -> CharacterStatRep(c, r, w) },
            byAscension = agg.byAscension.map { (a, r, w) -> AscensionStatRep(a, r, w) },
            topDeathCauses = agg.topDeathCauses.map { (c, n) -> DeathCauseStatRep(c, n) },
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

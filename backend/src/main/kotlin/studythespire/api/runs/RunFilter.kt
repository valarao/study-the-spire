@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)

package studythespire.api.runs

import java.util.Base64
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal data class RunFilter(
  val character: String? = null,
  val ascension: Int? = null,
  val status: RunStatus? = null,
  val from: Instant? = null,
  val to: Instant? = null,
)

internal data class RunCursor(val startTime: Instant, val runId: Uuid) {
  fun encode(): String =
    Base64.getUrlEncoder().withoutPadding()
      .encodeToString("${startTime}|$runId".toByteArray(Charsets.UTF_8))

  companion object {
    fun decode(s: String): RunCursor? = runCatching {
      val raw = String(Base64.getUrlDecoder().decode(s), Charsets.UTF_8)
      val (instant, id) = raw.split("|", limit = 2)
      RunCursor(Instant.parse(instant), Uuid.parse(id))
    }.getOrNull()
  }
}

internal data class RunPage(val runs: List<Run>, val nextCursor: String?)

/**
 * Internal aggregate result returned by [RunStore.summaryForUser]. The API feature
 * maps this into the public [StatsSummaryRep] DTO.
 */
internal data class SummaryAggregates(
  val totalRuns: Int,
  val wins: Int,
  val defeats: Int,
  val abandoned: Int,
  val totalRunTimeSecs: Long,
  /** (characterClass, runs, wins) sorted by runs DESC */
  val byCharacter: List<Triple<String?, Int, Int>>,
  /** (ascension, runs, wins) sorted by ascension ASC */
  val byAscension: List<Triple<Int, Int, Int>>,
  /** (cause, count) top 5 by count DESC */
  val topDeathCauses: List<Pair<String, Int>>,
)

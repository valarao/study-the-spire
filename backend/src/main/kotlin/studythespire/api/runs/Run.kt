@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.runs

import studythespire.api.users.UserId
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JvmInline
internal value class RunId(val value: Uuid) {
  override fun toString(): String = value.toString()
}

@JvmInline
internal value class RunImportId(val value: Uuid) {
  override fun toString(): String = value.toString()
}

internal enum class RunStatus(val value: String) {
  Victory("victory"),
  Defeat("defeat"),
  Abandoned("abandoned");

  companion object {
    fun derive(wasAbandoned: Boolean, win: Boolean): RunStatus = when {
      wasAbandoned -> Abandoned
      win -> Victory
      else -> Defeat
    }
  }
}

internal data class Run(
  val id: RunId,
  val importId: RunImportId,
  val userId: UserId,
  val status: RunStatus,
  val characterClass: String?,
  val ascension: Int,
  val seed: String,
  val buildId: String,
  val gameMode: String,
  val platformType: String,
  val startTime: Instant,
  val runTimeSecs: Int,
  val killedByEncounter: String?,
  val killedByEvent: String?,
  val schemaVersion: Int,
  val acts: List<String>,
)

internal data class RunWithRaw(val run: Run, val rawJson: String, val fileName: String?)

internal sealed interface ImportResult {
  data class Inserted(val runId: RunId) : ImportResult
  data class Duplicate(val runId: RunId) : ImportResult
}

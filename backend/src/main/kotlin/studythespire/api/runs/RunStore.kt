@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)

package studythespire.api.runs

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.Koin
import studythespire.api.users.UserId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class RunStore(
  koin: Koin,
) {
  private val database: R2dbcDatabase by lazy { koin.get() }
  private val json = Json { ignoreUnknownKeys = true }

  /**
   * Atomically inserts the raw upload row plus the normalized run row.
   * Returns [ImportResult.Duplicate] without mutating anything if a row already
   * exists for `(userId, sha256)`.
   */
  suspend fun importRun(
    userId: UserId,
    sha256: String,
    fileName: String?,
    rawJson: String,
  ): ImportResult = suspendTransaction(db = database) {
    val existing = RunImportsTable
      .select(RunImportsTable.id)
      .where { (RunImportsTable.userId eq userId.value) and (RunImportsTable.sha256 eq sha256) }
      .firstOrNull()
    if (existing != null) {
      val importId = existing[RunImportsTable.id]
      val runRow = RunsTable
        .select(RunsTable.id)
        .where { RunsTable.importId eq importId }
        .firstOrNull()
        ?: error("run row missing for existing import $importId")
      return@suspendTransaction ImportResult.Duplicate(RunId(runRow[RunsTable.id]))
    }

    val parsed = parseRunFile(rawJson)
    val now = Clock.System.now()
    val newImportId = Uuid.random()
    val newRunId = Uuid.random()

    RunImportsTable.insert {
      it[RunImportsTable.id] = newImportId
      it[RunImportsTable.userId] = userId.value
      it[RunImportsTable.sha256] = sha256
      it[RunImportsTable.fileName] = fileName
      it[RunImportsTable.rawJson] = rawJson
      it[RunImportsTable.receivedAt] = now
    }
    RunsTable.insert {
      it[RunsTable.id] = newRunId
      it[RunsTable.importId] = newImportId
      it[RunsTable.userId] = userId.value
      it[RunsTable.status] = parsed.status.value
      it[RunsTable.characterClass] = parsed.characterClass
      it[RunsTable.ascension] = parsed.ascension
      it[RunsTable.seed] = parsed.seed
      it[RunsTable.buildId] = parsed.buildId
      it[RunsTable.gameMode] = parsed.gameMode
      it[RunsTable.platformType] = parsed.platformType
      it[RunsTable.startTime] = parsed.startTime
      it[RunsTable.runTimeSecs] = parsed.runTimeSecs
      it[RunsTable.killedByEncounter] = parsed.killedByEncounter
      it[RunsTable.killedByEvent] = parsed.killedByEvent
      it[RunsTable.schemaVersion] = parsed.schemaVersion
      it[RunsTable.acts] = parsed.acts
    }
    ImportResult.Inserted(RunId(newRunId))
  }

  suspend fun listForUser(
    userId: UserId,
    filter: RunFilter = RunFilter(),
    cursor: RunCursor? = null,
    limit: Int = 25,
  ): RunPage = suspendTransaction(db = database) {
    val rows = RunsTable
      .select(
        RunsTable.id, RunsTable.importId, RunsTable.userId, RunsTable.status,
        RunsTable.characterClass, RunsTable.ascension, RunsTable.seed, RunsTable.buildId,
        RunsTable.gameMode, RunsTable.platformType, RunsTable.startTime, RunsTable.runTimeSecs,
        RunsTable.killedByEncounter, RunsTable.killedByEvent, RunsTable.schemaVersion, RunsTable.acts,
      )
      .where {
        var op: org.jetbrains.exposed.v1.core.Op<Boolean> = RunsTable.userId eq userId.value
        filter.character?.let { c -> op = op and (RunsTable.characterClass eq c) }
        filter.ascension?.let { a -> op = op and (RunsTable.ascension eq a) }
        filter.status?.let { s -> op = op and (RunsTable.status eq s.value) }
        filter.from?.let { f -> op = op and (RunsTable.startTime greaterEq f) }
        filter.to?.let { t -> op = op and (RunsTable.startTime less t) }
        cursor?.let { c ->
          // Older than the cursor; tie-break on id when start_time matches.
          op = op and (
            (RunsTable.startTime less c.startTime) or
              ((RunsTable.startTime eq c.startTime) and (RunsTable.id less c.runId))
          )
        }
        op
      }
      .orderBy(RunsTable.startTime to SortOrder.DESC, RunsTable.id to SortOrder.DESC)
      .limit(limit + 1)
      .toList()
    val hasMore = rows.size > limit
    val page = (if (hasMore) rows.dropLast(1) else rows).map { it.toRun() }
    val nextCursor = if (hasMore && page.isNotEmpty()) {
      val last = page.last()
      RunCursor(last.startTime, last.id.value).encode()
    } else null
    RunPage(runs = page, nextCursor = nextCursor)
  }

  suspend fun summaryForUser(userId: UserId): SummaryAggregates =
    suspendTransaction(db = database) {
      val rows = RunsTable
        .select(
          RunsTable.status, RunsTable.characterClass, RunsTable.ascension,
          RunsTable.runTimeSecs, RunsTable.killedByEncounter, RunsTable.killedByEvent,
        )
        .where { RunsTable.userId eq userId.value }
        .toList()
      var total = 0
      var wins = 0
      var defeats = 0
      var abandoned = 0
      var totalRunTime = 0L
      val byChar = mutableMapOf<String?, IntArray>()         // [runs, wins]
      val byAsc = mutableMapOf<Int, IntArray>()              // [runs, wins]
      val deathCauses = mutableMapOf<String, Int>()
      for (r in rows) {
        total += 1
        val s = r[RunsTable.status]
        val isWin = s == RunStatus.Victory.value
        when (s) {
          RunStatus.Victory.value -> wins += 1
          RunStatus.Defeat.value -> defeats += 1
          RunStatus.Abandoned.value -> abandoned += 1
        }
        totalRunTime += r[RunsTable.runTimeSecs]
        val ch = r[RunsTable.characterClass]
        val cArr = byChar.getOrPut(ch) { IntArray(2) }
        cArr[0] += 1; if (isWin) cArr[1] += 1
        val asc = r[RunsTable.ascension]
        val aArr = byAsc.getOrPut(asc) { IntArray(2) }
        aArr[0] += 1; if (isWin) aArr[1] += 1
        if (!isWin) {
          val cause = causeOf(
            status = s,
            killedByEncounter = r[RunsTable.killedByEncounter],
            killedByEvent = r[RunsTable.killedByEvent],
          )
          if (cause != null) deathCauses[cause] = (deathCauses[cause] ?: 0) + 1
        }
      }
      SummaryAggregates(
        totalRuns = total,
        wins = wins,
        defeats = defeats,
        abandoned = abandoned,
        totalRunTimeSecs = totalRunTime,
        byCharacter = byChar.entries
          .sortedByDescending { it.value[0] }
          .map { Triple(it.key, it.value[0], it.value[1]) },
        byAscension = byAsc.entries
          .sortedBy { it.key }
          .map { Triple(it.key, it.value[0], it.value[1]) },
        topDeathCauses = deathCauses.entries
          .sortedByDescending { it.value }
          .take(5)
          .map { it.key to it.value },
      )
    }

  private fun causeOf(status: String, killedByEncounter: String?, killedByEvent: String?): String? {
    val enc = killedByEncounter?.takeUnless { it == "NONE.NONE" }
    val ev = killedByEvent?.takeUnless { it == "NONE.NONE" }
    if (enc != null) return enc
    if (ev != null) return ev
    if (status == RunStatus.Abandoned.value) return "abandoned"
    return null
  }

  suspend fun findById(runId: RunId, userId: UserId): RunWithRaw? =
    suspendTransaction(db = database) {
      val row = (RunsTable innerJoin RunImportsTable)
        .select(
          RunsTable.id, RunsTable.importId, RunsTable.userId, RunsTable.status,
          RunsTable.characterClass, RunsTable.ascension, RunsTable.seed, RunsTable.buildId,
          RunsTable.gameMode, RunsTable.platformType, RunsTable.startTime, RunsTable.runTimeSecs,
          RunsTable.killedByEncounter, RunsTable.killedByEvent, RunsTable.schemaVersion, RunsTable.acts,
          RunImportsTable.rawJson, RunImportsTable.fileName,
        )
        .where { (RunsTable.id eq runId.value) and (RunsTable.userId eq userId.value) }
        .firstOrNull()
        ?: return@suspendTransaction null
      RunWithRaw(
        run = row.toRun(),
        rawJson = row[RunImportsTable.rawJson],
        fileName = row[RunImportsTable.fileName],
      )
    }

  private fun parseRunFile(rawJson: String): ParsedRun {
    val obj = json.parseToJsonElement(rawJson).jsonObject
    val wasAbandoned = obj.requireBool("was_abandoned")
    val win = obj.requireBool("win")
    val players = obj["players"] as? JsonArray
    val firstPlayer = players?.firstOrNull() as? JsonObject
    val characterClass = firstPlayer?.optString("character")

    return ParsedRun(
      status = RunStatus.derive(wasAbandoned = wasAbandoned, win = win),
      characterClass = characterClass,
      ascension = obj.requireInt("ascension"),
      seed = obj.requireString("seed"),
      buildId = obj.requireString("build_id"),
      gameMode = obj.requireString("game_mode"),
      platformType = obj.requireString("platform_type"),
      startTime = Instant.fromEpochSeconds(obj.requireLong("start_time")),
      runTimeSecs = obj.requireInt("run_time"),
      killedByEncounter = obj.optString("killed_by_encounter"),
      killedByEvent = obj.optString("killed_by_event"),
      schemaVersion = obj.requireInt("schema_version"),
      acts = (obj["acts"] as? JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull() } ?: emptyList(),
    )
  }
}

private data class ParsedRun(
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

private fun JsonObject.requireString(key: String): String =
  this[key]?.jsonPrimitive?.contentOrNull()
    ?: error("run-file missing required string field '$key'")

private fun JsonObject.requireInt(key: String): Int =
  this[key]?.jsonPrimitive?.int ?: error("run-file missing required int field '$key'")

private fun JsonObject.requireLong(key: String): Long =
  this[key]?.jsonPrimitive?.long ?: error("run-file missing required long field '$key'")

private fun JsonObject.requireBool(key: String): Boolean =
  this[key]?.jsonPrimitive?.boolean ?: error("run-file missing required bool field '$key'")

private fun JsonObject.optString(key: String): String? =
  this[key]?.jsonPrimitive?.contentOrNull()

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
  if (isString) content else null

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun org.jetbrains.exposed.v1.core.ResultRow.toRun(): Run = Run(
  id = RunId(this[RunsTable.id]),
  importId = RunImportId(this[RunsTable.importId]),
  userId = UserId(this[RunsTable.userId]),
  status = RunStatus.entries.first { it.value == this[RunsTable.status] },
  characterClass = this[RunsTable.characterClass],
  ascension = this[RunsTable.ascension],
  seed = this[RunsTable.seed],
  buildId = this[RunsTable.buildId],
  gameMode = this[RunsTable.gameMode],
  platformType = this[RunsTable.platformType],
  startTime = this[RunsTable.startTime],
  runTimeSecs = this[RunsTable.runTimeSecs],
  killedByEncounter = this[RunsTable.killedByEncounter],
  killedByEvent = this[RunsTable.killedByEvent],
  schemaVersion = this[RunsTable.schemaVersion],
  acts = this[RunsTable.acts],
)

package studythespire.api.runs

import kairo.rest.Rest
import kairo.rest.RestEndpoint

internal data class RunRep(
  val id: String,
  val status: String,
  val characterClass: String?,
  val ascension: Int,
  val seed: String,
  val buildId: String,
  val gameMode: String,
  val platformType: String,
  val startTime: String,
  val runTimeSecs: Int,
  val killedByEncounter: String?,
  val killedByEvent: String?,
  val schemaVersion: Int,
  val acts: List<String>,
)

internal data class RunsListRep(
  val runs: List<RunRep>,
  val nextCursor: String?,
)

internal data class RunDetailRep(
  val run: RunRep,
  val rawJson: String,
  val fileName: String?,
)

internal data class ImportRunFileRep(
  val imported: Boolean,
  val runId: String,
)

internal data class CharacterStatRep(val characterClass: String?, val runs: Int, val wins: Int)
internal data class AscensionStatRep(val ascension: Int, val runs: Int, val wins: Int)
internal data class DeathCauseStatRep(val cause: String, val count: Int)

internal data class StatsSummaryRep(
  val totalRuns: Int,
  val wins: Int,
  val defeats: Int,
  val abandoned: Int,
  val winRate: Double?,
  val avgRunTimeSecs: Int?,
  val byCharacter: List<CharacterStatRep>,
  val byAscension: List<AscensionStatRep>,
  val topDeathCauses: List<DeathCauseStatRep>,
)

internal object RunsApi {
  @Rest("GET", "/runs")
  @Rest.Accept("application/json")
  data class List(
    @QueryParam val character: String? = null,
    @QueryParam val ascension: Int? = null,
    @QueryParam val status: String? = null,
    @QueryParam val from: String? = null,
    @QueryParam val to: String? = null,
    @QueryParam val cursor: String? = null,
    @QueryParam val limit: Int? = null,
  ) : RestEndpoint<Unit, RunsListRep>()

  @Rest("GET", "/runs/:runId")
  @Rest.Accept("application/json")
  data class Get(@PathParam val runId: String) : RestEndpoint<Unit, RunDetailRep>()
}

internal object StatsApi {
  @Rest("GET", "/stats/summary")
  @Rest.Accept("application/json")
  data object Summary : RestEndpoint<Unit, StatsSummaryRep>()
}

internal object ImportsApi {
  @Rest("POST", "/imports/run-file")
  @Rest.ContentType("application/json")
  @Rest.Accept("application/json")
  data class Post(
    override val body: String,
  ) : RestEndpoint<String, ImportRunFileRep>()
}

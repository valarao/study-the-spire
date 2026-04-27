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

internal data class RunsListRep(val runs: List<RunRep>)

internal data class RunDetailRep(
  val run: RunRep,
  val rawJson: String,
  val fileName: String?,
)

internal data class ImportRunFileRep(
  val imported: Boolean,
  val runId: String,
)

internal object RunsApi {
  @Rest("GET", "/runs")
  @Rest.Accept("application/json")
  data object List : RestEndpoint<Unit, RunsListRep>()

  @Rest("GET", "/runs/:runId")
  @Rest.Accept("application/json")
  data class Get(@PathParam val runId: String) : RestEndpoint<Unit, RunDetailRep>()
}

internal object ImportsApi {
  @Rest("POST", "/imports/run-file")
  @Rest.ContentType("application/json")
  @Rest.Accept("application/json")
  data class Post(
    override val body: String,
  ) : RestEndpoint<String, ImportRunFileRep>()
}

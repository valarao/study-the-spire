@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.runs

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.datetime.timestamp
import studythespire.api.users.UsersTable

internal object RunsTable : Table("runs") {
  val id = uuid("id")
  val importId = uuid("import_id").references(RunImportsTable.id)
  val userId = uuid("user_id").references(UsersTable.id)
  val status = text("status")
  val characterClass = text("character_class").nullable()
  val ascension = integer("ascension")
  val seed = text("seed")
  val buildId = text("build_id")
  val gameMode = text("game_mode")
  val platformType = text("platform_type")
  val startTime = timestamp("start_time")
  val runTimeSecs = integer("run_time_secs")
  val killedByEncounter = text("killed_by_encounter").nullable()
  val killedByEvent = text("killed_by_event").nullable()
  val schemaVersion = integer("schema_version")
  val acts = array("acts", TextColumnType())
  override val primaryKey = PrimaryKey(id)
}

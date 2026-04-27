@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.runs

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import studythespire.api.users.UsersTable

internal object RunImportsTable : Table("run_imports") {
  val id = uuid("id")
  val userId = uuid("user_id").references(UsersTable.id)
  val sha256 = text("sha256")
  val fileName = text("file_name").nullable()
  val rawJson = text("raw_json")
  val receivedAt = timestamp("received_at")
  override val primaryKey = PrimaryKey(id)
}

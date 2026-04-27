@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.tokens

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import studythespire.api.users.UsersTable

internal object UploadTokensTable : Table("upload_tokens") {
  val id = uuid("id")
  val userId = uuid("user_id").references(UsersTable.id)
  val name = text("name")
  val tokenHash = text("token_hash")
  val tokenPrefix = text("token_prefix")
  val createdAt = timestamp("created_at")
  val lastUsedAt = timestamp("last_used_at").nullable()
  val revokedAt = timestamp("revoked_at").nullable()
  override val primaryKey = PrimaryKey(id)
}

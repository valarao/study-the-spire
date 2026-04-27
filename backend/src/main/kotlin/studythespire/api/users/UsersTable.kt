@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.users

import org.jetbrains.exposed.v1.core.Table

internal object UsersTable : Table("users") {
  val id = uuid("id")
  val clerkUserId = text("clerk_user_id")
  val email = text("email").nullable()
  override val primaryKey = PrimaryKey(id)
}

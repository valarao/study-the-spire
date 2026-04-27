@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.users

import kotlin.uuid.Uuid

@JvmInline
internal value class UserId(val value: Uuid) {
  override fun toString(): String = value.toString()
}

internal data class User(
  val id: UserId,
  val clerkUserId: String,
  val email: String?,
)

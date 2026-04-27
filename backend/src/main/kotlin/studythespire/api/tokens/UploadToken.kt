@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.tokens

import studythespire.api.users.UserId
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JvmInline
internal value class UploadTokenId(val value: Uuid) {
  override fun toString(): String = value.toString()
}

internal data class UploadToken(
  val id: UploadTokenId,
  val userId: UserId,
  val name: String,
  val tokenPrefix: String,
  val createdAt: Instant,
  val lastUsedAt: Instant?,
  val revokedAt: Instant?,
)

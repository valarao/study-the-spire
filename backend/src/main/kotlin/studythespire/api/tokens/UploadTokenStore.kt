@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.tokens

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.koin.core.Koin
import studythespire.api.users.UserId
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

internal class UploadTokenStore(
  koin: Koin,
) {
  private val database: R2dbcDatabase by lazy { koin.get() }

  /** Creates a token, returns the domain object plus the raw secret (shown to the user only here). */
  @OptIn(ExperimentalTime::class)
  suspend fun create(userId: UserId, name: String): Pair<UploadToken, String> {
    val secret = UploadTokenSecret.generate()
    val hash = UploadTokenSecret.hash(secret)
    val prefix = UploadTokenSecret.prefix(secret)
    val now = Clock.System.now()
    val newId = Uuid.random()
    suspendTransaction(db = database) {
      UploadTokensTable.insert {
        it[UploadTokensTable.id] = newId
        it[UploadTokensTable.userId] = userId.value
        it[UploadTokensTable.name] = name
        it[UploadTokensTable.tokenHash] = hash
        it[UploadTokensTable.tokenPrefix] = prefix
        it[UploadTokensTable.createdAt] = now
      }
    }
    return UploadToken(
      id = UploadTokenId(newId),
      userId = userId,
      name = name,
      tokenPrefix = prefix,
      createdAt = now,
      lastUsedAt = null,
      revokedAt = null,
    ) to secret
  }

  /** Lists active (non-revoked) tokens for a user, newest first. */
  suspend fun listForUser(userId: UserId): List<UploadToken> =
    suspendTransaction(db = database) {
      UploadTokensTable
        .select(
          UploadTokensTable.id,
          UploadTokensTable.userId,
          UploadTokensTable.name,
          UploadTokensTable.tokenPrefix,
          UploadTokensTable.createdAt,
          UploadTokensTable.lastUsedAt,
          UploadTokensTable.revokedAt,
        )
        .where { (UploadTokensTable.userId eq userId.value) and UploadTokensTable.revokedAt.isNull() }
        .orderBy(UploadTokensTable.createdAt, SortOrder.DESC)
        .toList()
        .map { it.toUploadToken() }
    }

  /** Looks up a token by its SHA-256 hash. Returns null if not found or revoked. */
  suspend fun findByHash(tokenHash: String): UploadToken? =
    suspendTransaction(db = database) {
      UploadTokensTable
        .select(
          UploadTokensTable.id,
          UploadTokensTable.userId,
          UploadTokensTable.name,
          UploadTokensTable.tokenPrefix,
          UploadTokensTable.createdAt,
          UploadTokensTable.lastUsedAt,
          UploadTokensTable.revokedAt,
        )
        .where { UploadTokensTable.tokenHash eq tokenHash }
        .firstOrNull()
        ?.toUploadToken()
        ?.takeIf { it.revokedAt == null }
    }

  /** Marks a token as revoked. Filtered by userId so users can only revoke their own tokens. */
  @OptIn(ExperimentalTime::class)
  suspend fun revoke(id: UploadTokenId, userId: UserId): Boolean =
    suspendTransaction(db = database) {
      val rows = UploadTokensTable.update({
        (UploadTokensTable.id eq id.value) and
          (UploadTokensTable.userId eq userId.value) and
          UploadTokensTable.revokedAt.isNull()
      }) {
        it[UploadTokensTable.revokedAt] = Clock.System.now()
      }
      rows > 0
    }

  /** Best-effort update of lastUsedAt. Failures are swallowed by the caller. */
  @OptIn(ExperimentalTime::class)
  suspend fun touchLastUsed(id: UploadTokenId) {
    suspendTransaction(db = database) {
      UploadTokensTable.update({ UploadTokensTable.id eq id.value }) {
        it[UploadTokensTable.lastUsedAt] = Clock.System.now()
      }
    }
  }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun org.jetbrains.exposed.v1.core.ResultRow.toUploadToken(): UploadToken =
  UploadToken(
    id = UploadTokenId(this[UploadTokensTable.id]),
    userId = UserId(this[UploadTokensTable.userId]),
    name = this[UploadTokensTable.name],
    tokenPrefix = this[UploadTokensTable.tokenPrefix],
    createdAt = this[UploadTokensTable.createdAt],
    lastUsedAt = this[UploadTokensTable.lastUsedAt],
    revokedAt = this[UploadTokensTable.revokedAt],
  )

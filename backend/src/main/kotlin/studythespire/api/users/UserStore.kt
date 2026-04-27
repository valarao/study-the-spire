@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package studythespire.api.users

import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.koin.core.Koin

internal class UserStore(
  koin: Koin,
) {
  private val database: R2dbcDatabase by lazy { koin.get() }

  suspend fun getOrCreate(clerkUserId: String, email: String?): User =
    suspendTransaction(db = database) {
      val existing = UsersTable
        .select(UsersTable.id, UsersTable.clerkUserId, UsersTable.email)
        .where { UsersTable.clerkUserId eq clerkUserId }
        .firstOrNull()
      if (existing != null) {
        if (email != null && existing[UsersTable.email] != email) {
          UsersTable.update({ UsersTable.clerkUserId eq clerkUserId }) {
            it[UsersTable.email] = email
          }
        }
        return@suspendTransaction User(
          id = UserId(existing[UsersTable.id]),
          clerkUserId = existing[UsersTable.clerkUserId],
          email = email ?: existing[UsersTable.email],
        )
      }
      UsersTable.insert {
        it[UsersTable.clerkUserId] = clerkUserId
        it[UsersTable.email] = email
      }
      val row = UsersTable
        .select(UsersTable.id, UsersTable.clerkUserId, UsersTable.email)
        .where { UsersTable.clerkUserId eq clerkUserId }
        .firstOrNull()
        ?: error("User row missing immediately after insert: $clerkUserId")
      User(
        id = UserId(row[UsersTable.id]),
        clerkUserId = row[UsersTable.clerkUserId],
        email = row[UsersTable.email],
      )
    }
}

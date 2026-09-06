package com.jameeli.thykra

import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.RefreshTokensTable
import com.jameeli.thykra.db.tables.UsersTable

import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * All server tests share ONE named in-memory H2 database. Exposed's default
 * database (and its per-thread transaction managers) are process-global mutable
 * state; giving every test class its own database makes reused coroutine worker
 * threads occasionally resolve a stale manager pointing at a dead database.
 * With a single shared database every manager resolves to live storage. Tests
 * isolate through distinct emails/ids rather than by wiping tables.
 */
object TestDatabase {
    const val URL = "jdbc:h2:mem:thykra-tests;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"

    fun connect() {
        Database.connect(URL, driver = "org.h2.Driver", user = "sa", password = "")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(UsersTable, RefreshTokensTable)
        }
    }
}

package com.jameeli.thykra.db

import com.jameeli.thykra.db.tables.ActivitySeenTable
import com.jameeli.thykra.db.tables.AlbumInvitesTable
import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.db.tables.BlockedMembersTable
import com.jameeli.thykra.db.tables.CommentsTable
import com.jameeli.thykra.db.tables.InviteJoinsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.db.tables.RecapsTable
import com.jameeli.thykra.db.tables.RefreshTokensTable
import com.jameeli.thykra.db.tables.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.ApplicationEnvironment
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    private var dataSource: HikariDataSource? = null
    private var database: Database? = null

    fun init(environment: ApplicationEnvironment) {
        val config = environment.config
        val driver = config.property("database.driver").getString()
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.property("database.url").getString()
            driverClassName = driver
            username = config.property("database.user").getString()
            password = config.property("database.password").getString()
            maximumPoolSize = config.property("database.maxPoolSize").getString().toInt()
            isAutoCommit = false
            if (driver.contains("postgresql", ignoreCase = true)) {
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            }
            validate()
        }

        val ds = HikariDataSource(hikariConfig)
        dataSource = ds
        database = Database.connect(ds)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable, RefreshTokensTable, AlbumsTable, AlbumMembersTable, AlbumInvitesTable,
                MediaTable, ReactionsTable, CommentsTable, BlockedMembersTable,
                InviteJoinsTable, ActivitySeenTable, RecapsTable
            )
        }
    }

    /**
     * Unregisters the database from Exposed and closes the pool (shutting down
     * an H2 in-memory database, which otherwise lives until JVM exit).
     *
     * Unregistering matters: Exposed resolves transactions against its oldest
     * registered database by default, so a test JVM that boots several apps
     * would silently keep routing queries to the first one. Used by the test
     * harness; production never calls it.
     */
    fun close() {
        val db = database
        val ds = dataSource
        database = null
        dataSource = null
        if (db != null) runCatching { TransactionManager.closeAndUnregister(db) }
        if (ds != null) {
            runCatching {
                if (ds.jdbcUrl.startsWith("jdbc:h2:mem:")) {
                    ds.connection.use { it.createStatement().execute("SHUTDOWN") }
                }
            }
            runCatching { ds.close() }
        }
    }
}

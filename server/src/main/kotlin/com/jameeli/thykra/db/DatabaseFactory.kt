package com.jameeli.thykra.db

import com.jameeli.thykra.db.tables.AlbumInvitesTable
import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.db.tables.RefreshTokensTable
import com.jameeli.thykra.db.tables.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.ApplicationEnvironment
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

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

        Database.connect(HikariDataSource(hikariConfig))

        transaction {
            SchemaUtils.create(
                UsersTable, RefreshTokensTable, AlbumsTable, AlbumMembersTable, AlbumInvitesTable,
                MediaTable, ReactionsTable
            )
        }
    }
}

package database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseManager {
    private val logger = LoggerFactory.getLogger(DatabaseManager::class.java)
    private lateinit var dataSource: HikariDataSource

    fun init(jdbcUrl: String) {
        logger.info("Initializing database connection...")

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 600000 // 10 minutes
            connectionTimeout = 30000 // 30 seconds
            maxLifetime = 1800000 // 30 minutes
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        logger.info("Database connection established")

        runMigrations()
    }

    private fun runMigrations() {
        logger.info("Running database migrations...")

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()

        val result = flyway.migrate()

        logger.info("Database migrations complete: ${result.migrationsExecuted} migrations executed")
    }

    fun close() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            logger.info("Closing database connection...")
            dataSource.close()
        }
    }
}

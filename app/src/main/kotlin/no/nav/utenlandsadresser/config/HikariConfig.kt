package no.nav.utenlandsadresser.config

import com.zaxxer.hikari.HikariConfig
import org.postgresql.Driver

fun hikariConfig(config: UtenlandsadresserDatabaseConfig): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password.value
        driverClassName = Driver::class.qualifiedName
        maximumPoolSize = 1
        minimumIdle = 1
    }

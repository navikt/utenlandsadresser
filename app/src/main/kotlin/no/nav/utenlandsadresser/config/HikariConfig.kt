package no.nav.utenlandsadresser.config

import com.zaxxer.hikari.HikariConfig
import org.postgresql.Driver

fun hikariConfig(config: UtenlandsadresserDatabaseConfig): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = config.jdbcUrl.value
        username = config.username
        password = config.password.value
        driverClassName = Driver::class.qualifiedName
        maximumPoolSize = 10
        minimumIdle = 5
    }

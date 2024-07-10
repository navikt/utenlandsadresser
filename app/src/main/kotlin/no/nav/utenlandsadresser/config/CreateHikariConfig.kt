package no.nav.utenlandsadresser.config

import com.zaxxer.hikari.HikariConfig

fun createHikariConfig(config: UtenlandsadresserDatabaseConfig): HikariConfig {
    return HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password?.value
        driverClassName = config.driverClassName
        maximumPoolSize = 10
        minimumIdle = 5
    }
}
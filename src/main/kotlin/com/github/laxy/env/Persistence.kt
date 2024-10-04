package com.github.laxy.env

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.github.laxy.sqldelight.SqlDelight
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

fun hikari(env: Env.DataSource) =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = env.url
            username = env.username
            password = env.password
            driverClassName = env.driver
        }
    )

fun sqlDelight(dataSource: DataSource): SqlDelight {
    val driver = dataSource.asJdbcDriver()
    SqlDelight.Schema.create(driver)
    return SqlDelight(
        driver,
    )
}

package com.github.laxy.env

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.github.laxy.persistence.UserId
import com.github.laxy.sqldelight.SqlDelight
import com.github.laxy.sqldelight.Users
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
    return SqlDelight(driver, Users.Adapter(userIdAdapter))
}

private val userIdAdapter = columnAdapter(::UserId, UserId::serial)

private inline fun <A : Any, B> columnAdapter(
    crossinline decode: (databaseValue: B) -> A,
    crossinline encode: (value: A) -> B
): ColumnAdapter<A, B> =
    object : ColumnAdapter<A, B> {
        override fun decode(databaseValue: B): A = decode(databaseValue)

        override fun encode(value: A): B = encode(value)
    }

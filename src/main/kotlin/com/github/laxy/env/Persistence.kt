package com.github.laxy.env

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.closeable
import arrow.fx.coroutines.continuations.ResourceScope
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserThemeId
import com.github.laxy.sqldelight.SqlDelight
import com.github.laxy.sqldelight.User_themes
import com.github.laxy.sqldelight.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

suspend fun ResourceScope.hikari(env: Env.DataSource) = autoCloseable {
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = env.url
            username = env.username
            password = env.password
            driverClassName = env.driver
        }
    )
}

suspend fun ResourceScope.sqlDelight(dataSource: DataSource): SqlDelight {
    val driver = closeable { dataSource.asJdbcDriver() }
    SqlDelight.Schema.create(driver)
    return SqlDelight(driver, User_themes.Adapter(userThemeIdAdapter), Users.Adapter(userIdAdapter))
}

private val userIdAdapter = columnAdapter(::UserId, UserId::serial)
private val userThemeIdAdapter = columnAdapter(::UserThemeId, UserThemeId::serial)

private inline fun <A : Any, B> columnAdapter(
    crossinline decode: (databaseValue: B) -> A,
    crossinline encode: (value: A) -> B
): ColumnAdapter<A, B> =
    object : ColumnAdapter<A, B> {
        override fun decode(databaseValue: B): A = decode(databaseValue)

        override fun encode(value: A): B = encode(value)
    }

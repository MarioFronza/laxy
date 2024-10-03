package com.github.laxy.env

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun hikari(env: Env.DataSource) =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = env.url
            username = env.username
            password = env.password
            driverClassName = env.driver
        }
    )

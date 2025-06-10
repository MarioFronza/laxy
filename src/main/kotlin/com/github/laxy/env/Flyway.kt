package com.github.laxy.env

import javax.sql.DataSource
import org.flywaydb.core.Flyway

fun flyway(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}

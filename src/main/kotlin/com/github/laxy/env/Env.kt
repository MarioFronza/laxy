package com.github.laxy.env

import java.lang.System.getenv

private const val PORT: Int = 8080
private const val JDBC_URL: String = "jdbc:postgresql://localhost:5432/laxy-database"
private const val JDBC_USER: String = "postgres"
private const val JDBC_PASS: String = "postgres"
private const val JDBC_DRIVER: String = "org.postgresql.Driver"

data class Env(val http: Http = Http(), val dataSource: DataSource = DataSource()) {
    data class Http(
        val host: String = getenv("HOST") ?: "0.0.0.0",
        val port: Int = getenv("SERVER_PORT")?.toIntOrNull() ?: PORT
    )

    data class DataSource(
        val url: String = getenv("POSTGRES_URL") ?: JDBC_URL,
        val username: String = getenv("POSTGRES_USERNAME") ?: JDBC_USER,
        val password: String = getenv("POSTGRES_PASSWORD") ?: JDBC_PASS,
        val driver: String = JDBC_DRIVER
    )
}

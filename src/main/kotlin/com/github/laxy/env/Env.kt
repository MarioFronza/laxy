package com.github.laxy.env

import java.lang.System.getenv
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private const val PORT: Int = 8080
private const val JDBC_URL: String = "jdbc:postgresql://localhost:5432/laxy-database"
private const val JDBC_USER: String = "postgres"
private const val JDBC_PASS: String = "postgres"
private const val JDBC_DRIVER: String = "org.postgresql.Driver"
private const val OPENAI_TOKEN: String = "token"
private const val AUTH_SECRET: String = "MySuperStrongSecret"
private const val AUTH_ISSUER: String = "LaxyIssuer"
private const val AUTH_DURATION: Int = 30
private const val OTEL_METRIC_INTERVAL = 10L

private const val OTEL_EXPORTER_ENDPOINT: String = "http://localhost:4317"

data class Env(
    val http: Http = Http(),
    val dataSource: DataSource = DataSource(),
    val openAI: OpenAI = OpenAI(),
    val auth: Auth = Auth(),
    val otel: OpenTelemetry = OpenTelemetry()
) {
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

    data class OpenAI(val token: String = getenv("OPENAI_TOKEN") ?: OPENAI_TOKEN)

    data class Auth(
        val secret: String = getenv("JWT_SECRET") ?: AUTH_SECRET,
        val issuer: String = getenv("JWT_ISSUER") ?: AUTH_ISSUER,
        val duration: Duration = (getenv("JWT_DURATION")?.toIntOrNull() ?: AUTH_DURATION).days
    )

    data class OpenTelemetry(
        val endpoint: String = getenv("OTEL_EXPORTER_ENDPOINT") ?: OTEL_EXPORTER_ENDPOINT,
        val serviceName: String = getenv("OTEL_SERVICE_NAME") ?: "laxy",
        val metricIntervalSeconds: Long =
            getenv("OTEL_METRIC_INTERVAL")?.toLongOrNull() ?: OTEL_METRIC_INTERVAL
    )
}

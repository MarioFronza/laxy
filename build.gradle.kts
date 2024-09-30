import com.github.laxy.setupDetekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id(libs.plugins.kotlin.jvm.pluginId)
    id(libs.plugins.detekt.pluginId)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

application {
    mainClass = "com.github.laxy.MainKt"
}

sqldelight {
    databases {
        create("SqlDelight"){
            packageName = "com.github.laxy.sqldelight"
            dialect(libs.sqldelight.postgresql.get())
        }
    }
}

allprojects {
    extra.set("dokka.outputDirectory", rootDir.resolve("docs"))
    setupDetekt()
}

repositories {
    mavenCentral()
}

tasks {
    test {
        useJUnitPlatform()
    }
}

ktor {
    docker {
        jreVersion = JavaVersion.VERSION_17
        localImageName = "laxy"
        imageTag = "latest"
    }
}

spotless {
    kotlin {
        targetExclude("**/build/**")
        ktfmt("0.46").googleStyle()
    }
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.kjwt.core)
    implementation(libs.logback.classic)
    implementation(libs.sqldelight.jdbc)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.bundles.cohort)

    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.bundles.kotest)
}

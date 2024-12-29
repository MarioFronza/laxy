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
    alias(libs.plugins.power.assert)
}

application {
    mainClass = "com.github.laxy.MainKt"
}

sqldelight {
    databases {
        create("SqlDelight") {
            packageName.set("com.github.laxy.sqldelight")
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
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

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
        ktfmt("0.46").kotlinlangStyle()
    }
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.suspendapp)
    implementation(libs.kjwt.core)
    implementation(libs.logback.classic)
    implementation(libs.sqldelight.jdbc)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.bundles.cohort)
    implementation(libs.openai)

    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.bundles.kotest)
}

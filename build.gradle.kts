plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.cloud.tools.jib") version "3.4.5"
    application
}

group = "me.centralhardware.telegram"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val ktgbotapiVersion = "29.0.0"
val ktorVersion = "3.3.1"
val exposedVersion = "0.56.0"

dependencies {
    implementation("dev.inmo:tgbotapi:$ktgbotapiVersion")
    implementation("com.github.centralhardware:ktgbotapi-commons:$ktgbotapiVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-restrict-access-middleware:$ktgbotapiVersion")

    // PDF parsing
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // Excel parsing (if needed)
    implementation("org.apache.poi:poi:5.3.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.10.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.15")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

jib {
    from {
        image = System.getenv("JIB_FROM_IMAGE") ?: "eclipse-temurin:24-jre"
    }
    to {
    }
    container {
        mainClass = "MainKt"
        jvmFlags = listOf("-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0")
        creationTime = "USE_CURRENT_TIMESTAMP"
        labels = mapOf(
            "org.opencontainers.image.source" to (System.getenv("GITHUB_SERVER_URL")?.let { server ->
                val repo = System.getenv("GITHUB_REPOSITORY")
                if (repo != null) "$server/$repo" else ""
            } ?: ""),
            "org.opencontainers.image.revision" to (System.getenv("GITHUB_SHA") ?: "")
        )
        user = "10001"
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}

application {
    mainClass.set("MainKt")
}

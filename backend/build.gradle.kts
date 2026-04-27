import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.3.10"
  application
  id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.0"
}

repositories {
  google()
  mavenCentral()
  maven {
    url = uri("artifactregistry://us-central1-maven.pkg.dev/highbeam-kairo/maven")
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

dependencies {
  implementation(platform("com.highbeam.kairo:bom-full:20260309.210412-888c0e27"))
  implementation("com.highbeam.kairo:kairo-application")
  implementation("com.highbeam.kairo:kairo-server")
  implementation("com.highbeam.kairo:kairo-rest-feature")
  implementation("com.highbeam.kairo:kairo-config")
  implementation("com.highbeam.kairo:kairo-health-check-feature")
  implementation("com.highbeam.kairo:kairo-dependency-injection-feature")
  implementation("com.highbeam.kairo:kairo-sql-feature")
  implementation("com.highbeam.kairo:kairo-sql-postgres")
  implementation("io.ktor:ktor-server-auth-jwt")
  implementation("com.auth0:java-jwt:4.5.0")
  implementation("com.auth0:jwks-rsa:0.22.1")
  runtimeOnly("org.postgresql:r2dbc-postgresql")
  // Used in production via the r2dbc:gcp:postgresql:// URL scheme to talk to Cloud SQL.
  runtimeOnly("com.google.cloud.sql:cloud-sql-connector-r2dbc-postgres:1.21.0")
}

application {
  mainClass = "studythespire.api.MainKt"
}

tasks.named<Jar>("jar") {
  manifest {
    attributes(
      "Main-Class" to "studythespire.api.MainKt",
    )
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xskip-prerelease-check")
  }
}

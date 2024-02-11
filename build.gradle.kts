import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.12"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
    kotlin("plugin.allopen") version "1.6.21"
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

group = "org.home.zaval"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

val springdocOpenapiVersion = "1.7.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // db
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql:42.6.0")
    runtimeOnly("org.flywaydb:flyway-core")

    // region Kotlin specifics
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // endregion Kotlin specifics

    // region Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // endregion Development

    // openapi
    implementation("org.springdoc:springdoc-openapi-ui:$springdocOpenapiVersion")

    // region Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // endregion Test
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

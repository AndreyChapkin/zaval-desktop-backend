import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.6"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

group = "org.home.zaval"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

val springdocOpenapiVersion = "2.1.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")

	// region Kotlin specifics
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	// endregion Kotlin specifics

	// region Development
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	// endregion Development

	// openapi
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion")

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

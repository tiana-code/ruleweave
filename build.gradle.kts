plugins {
    kotlin("jvm") version "1.9.24"
}

group = "com.ruleweave"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation(kotlin("test"))
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

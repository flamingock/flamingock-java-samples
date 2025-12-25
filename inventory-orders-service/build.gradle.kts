import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    java
    application
    idea
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

group = "io.flamingock"
version = "1.0-SNAPSHOT"

val flamingockVersion = "1.0.0-beta.7"
logger.lifecycle("Building with flamingock version: $flamingockVersion")

val mongodbVersion = "5.5.1"
val kafkaVersion = "3.7.0"
val avroVersion = "1.11.3"
val confluentVersion = "7.5.0"
val snakeyamlVersion = "2.2"

dependencies {
//    Flamingock Dependencies
    implementation(platform("io.flamingock:flamingock-community-bom:$flamingockVersion"))
    implementation("io.flamingock:flamingock-community")
    implementation("io.flamingock:flamingock-springboot-integration")


    // Optional: enable GraalVM native image support for Flamingock
    // See: https://docs.flamingock.io/frameworks/graalvm
    // Uncomment
    // implementation("io.flamingock:flamingock-graalvm:$flamingockVersion")
    annotationProcessor("io.flamingock:flamingock-processor:$flamingockVersion")

//    MongoDB dependencies
    implementation("org.mongodb:mongodb-driver-sync:$mongodbVersion")
    implementation("org.mongodb:mongodb-driver-core:$mongodbVersion")
    implementation("org.mongodb:bson:$mongodbVersion")

//    Kafka dependencies
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.confluent:kafka-schema-registry-client:$confluentVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")

//    Config file dependencies
    implementation("org.yaml:snakeyaml:$snakeyamlVersion")

//    HTTP client for LaunchDarkly Management API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

//    Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")

//    Others dependencies needed for this example
//    implementation("org.slf4j:slf4j-simple:2.0.6")  // Commented out - Spring Boot provides logging

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.flamingock:flamingock-springboot-test-support")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:mongodb:1.21.4")
    testImplementation("org.testcontainers:kafka:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}

application {
    mainClass = "io.flamingock.examples.inventory.InventoryOrdersApp"
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "io.flamingock.examples.inventory.InventoryOrdersApp"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("org.slf4j.simpleLogger.logFile", "System.out")
    testLogging {
        events(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
        )
    }
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

plugins {
    java
    application
    idea
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.flamingock") version "1.0.0"
}
logger.lifecycle("Building with flamingock version: 1.0.0")

flamingock {
    community()
    springboot()
//    graalvm() // See: https://docs.flamingock.io/frameworks/graalvm
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


repositories {
//        mavenLocal() //uncomment only for development
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

group = "io.flamingock"
version = "1.0-SNAPSHOT"


val mongodbVersion = "5.5.1"
val kafkaVersion = "3.9.1"
val avroVersion = "1.11.4"
val confluentVersion = "7.5.12"
val snakeyamlVersion = "2.2"

dependencies {
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
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("org.testcontainers:testcontainers-mongodb:2.0.2")
    testImplementation("org.testcontainers:testcontainers-kafka:2.0.2")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.2")
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

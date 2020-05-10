import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
}

group = "com.gustavkarlsson.snappier"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    get("main").java.srcDir("build/generated/source/proto/main/java")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("de.halfbit:knot3:3.1.1")
    implementation("io.reactivex.rxjava3:rxjava:3.0.3")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("com.google.protobuf:protobuf-java:3.6.1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}

protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.0.0"
    }
}

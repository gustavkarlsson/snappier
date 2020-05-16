import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.72"
}

group = "se.gustavkarlsson.snappier"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

allprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes")
    }
}

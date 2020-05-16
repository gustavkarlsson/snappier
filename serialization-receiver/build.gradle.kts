plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":message"))
    implementation(kotlin("stdlib-jdk8"))
}

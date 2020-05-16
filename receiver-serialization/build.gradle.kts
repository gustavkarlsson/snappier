plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-message"))
    implementation(kotlin("stdlib-jdk8"))
}

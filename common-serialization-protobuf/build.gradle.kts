plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common-message"))
    api(project(":common-protobuf"))
    api(kotlin("stdlib-jdk8"))
    implementation("com.google.protobuf:protobuf-java:3.12.0")
}

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common-message"))
    api(project(":sender-serialization"))
    implementation(project(":common-protobuf"))
    implementation(project(":common-serialization-protobuf"))
    api(kotlin("stdlib-jdk8"))
    implementation("com.google.protobuf:protobuf-java:3.6.1")
}

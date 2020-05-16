plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-message"))
    implementation(project(":sender-serialization"))
    implementation(project(":common-protobuf"))
    implementation(project(":common-serialization-protobuf"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.protobuf:protobuf-java:3.6.1")
}

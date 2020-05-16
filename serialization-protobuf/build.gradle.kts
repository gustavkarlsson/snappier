plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":message"))
    implementation(project(":serialization"))
    implementation(project(":protobuf"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.protobuf:protobuf-java:3.6.1")
}

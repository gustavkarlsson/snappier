plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":message"))
    implementation(project(":serialization-receiver"))
    implementation(project(":protobuf"))
    implementation(project(":serialization-protobuf-common"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.protobuf:protobuf-java:3.6.1")
}

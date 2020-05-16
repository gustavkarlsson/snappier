plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-message"))
    implementation(project(":common-protobuf"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.protobuf:protobuf-java:3.6.1")
}

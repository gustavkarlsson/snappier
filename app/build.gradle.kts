plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":common-message"))
    implementation(project(":sender-files"))
    implementation(project(":sender-files-buffered"))
    implementation(project(":sender-serialization"))
    implementation(project(":sender-serialization-protobuf"))
    implementation(project(":sender-connection"))
    implementation(project(":sender-connection-default"))
    implementation(project(":sender-statemachine"))
    implementation(project(":sender-statemachine-knot"))
    implementation(project(":receiver-files"))
    implementation(project(":receiver-files-default"))
    implementation(project(":receiver-serialization"))
    implementation(project(":receiver-serialization-protobuf"))
    implementation(project(":receiver-connection"))
    implementation(project(":receiver-connection-default"))
    implementation(project(":receiver-statemachine"))
    implementation(project(":receiver-statemachine-knot"))
    implementation("de.halfbit:knot3:3.1.1")
    implementation("io.reactivex.rxjava3:rxjava:3.0.3")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
}

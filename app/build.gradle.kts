plugins {
    kotlin("jvm")
    kotlin("kapt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-message"))
    implementation(project(":common-config"))
    implementation(project(":sender-files-buffered"))
    implementation(project(":sender-serialization-protobuf"))
    implementation(project(":sender-connection-default"))
    implementation(project(":sender-statemachine-knot"))
    implementation(project(":receiver-files-default"))
    implementation(project(":receiver-serialization-protobuf"))
    implementation(project(":receiver-connection-default"))
    implementation(project(":receiver-statemachine-knot"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("de.halfbit:knot3:3.1.1")
    implementation("io.reactivex.rxjava3:rxjava:3.0.3")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("com.google.dagger:dagger:2.27")
    kapt("com.google.dagger:dagger-compiler:2.27")
}

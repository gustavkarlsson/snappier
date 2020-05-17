plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common-message"))
    api(project(":receiver-statemachine"))
    api(project(":receiver-connection"))
    api(project(":receiver-files"))
    api(kotlin("stdlib-jdk8"))
    api("io.reactivex.rxjava3:rxjava:3.0.3")
    implementation("de.halfbit:knot3:3.1.1")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
}
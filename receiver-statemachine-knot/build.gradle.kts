plugins {
    kotlin("jvm")
    kotlin("kapt")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common-config"))
    api(project(":common-domain"))
    api(project(":receiver-statemachine"))
    api(project(":receiver-connection"))
    api(project(":receiver-files"))
    api(kotlin("stdlib-jdk8"))
    api("io.reactivex.rxjava3:rxjava:3.0.3")
    implementation("de.halfbit:knot3:3.1.1")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    api("com.google.dagger:dagger:2.27")
    kapt("com.google.dagger:dagger-compiler:2.27")
}

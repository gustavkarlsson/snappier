plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":common-message"))
    implementation(project(":receiver-connection"))
    implementation("io.reactivex.rxjava3:rxjava:3.0.3")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
}

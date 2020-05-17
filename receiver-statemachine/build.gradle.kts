plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(project(":common-message"))
    api(project(":common-domain"))
    api("io.reactivex.rxjava3:rxjava:3.0.3")
}

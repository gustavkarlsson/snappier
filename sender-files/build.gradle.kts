plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common-message"))
    api(kotlin("stdlib-jdk8"))
    api("io.reactivex.rxjava3:rxjava:3.0.3")
}

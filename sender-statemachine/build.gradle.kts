plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":common-message"))
    implementation("io.reactivex.rxjava3:rxjava:3.0.3")
}

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-message"))
    implementation(project(":sender-files"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.reactivex.rxjava3:rxjava:3.0.3")
}

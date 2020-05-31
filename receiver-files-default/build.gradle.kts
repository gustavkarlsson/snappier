plugins {
    kotlin("jvm")
    kotlin("kapt")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common-config"))
    api(project(":receiver-files"))
    api(kotlin("stdlib-jdk8"))
    api("io.reactivex.rxjava3:rxjava:3.0.3")
    api("com.google.dagger:dagger:2.27")
    kapt("com.google.dagger:dagger-compiler:2.27")
}

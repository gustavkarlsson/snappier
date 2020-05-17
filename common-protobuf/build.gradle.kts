import com.google.protobuf.gradle.*

plugins {
    java
    id("com.google.protobuf") version "0.8.12"
}

repositories {
    mavenCentral()
}

sourceSets {
    get("main").java.srcDir("build/generated/source/proto/main/java")
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.12.0")
}

protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.0.0"
    }
}

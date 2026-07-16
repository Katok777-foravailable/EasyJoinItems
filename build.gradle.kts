plugins {
    id("java")
}

group = "com.katok"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/releases/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.7")
}
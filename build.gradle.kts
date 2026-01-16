plugins {
    kotlin("jvm") version "2.1.10"
}

group = "de.c4vxl"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Discord API
    implementation("net.dv8tion:JDA:6.3.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.24")
}
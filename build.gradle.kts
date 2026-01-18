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

    // Serialization
    implementation("com.google.code.gson:gson:2.13.2")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "de.c4vxl.MainKt"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        configurations.runtimeClasspath.get().filter { it.name != "module-info.class" }
            .map { if (it.isDirectory) it else zipTree(it) }
    )
}
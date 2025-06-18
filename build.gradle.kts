plugins {
    id("application")
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "io.bridgekit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.nats:jnats:2.21.1")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("io.javalin:javalin:6.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.bridgekit.nats.sampleapp.Main")
}

tasks.test {
    useJUnitPlatform()
}

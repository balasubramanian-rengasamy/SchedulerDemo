plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.allopen") version "2.0.20"
    war
    id("fish.payara.micro-gradle-plugin") version "2.0.1"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "com.prime"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    compileOnly("org.eclipse.microprofile:microprofile:6.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

allOpen {
    annotations(
        "jakarta.ws.rs.ApplicationPath",
        "jakarta.ejb.Stateless",
        "jakarta.ejb.Singleton",
        "jakarta.enterprise.context.RequestScoped",
        "jakarta.enterprise.context.ApplicationScoped",
        "jakarta.enterprise.context.SessionScoped",
        "jakarta.enterprise.context.Dependent",
        "jakarta.persistence.Entity",
        "jakarta.enterprise.inject.Model",
        "jakarta.ws.rs.Path",
        "jakarta.inject.Named"
    )
}

payaraMicro {
    payaraVersion = "6.2024.10"
    isDeployWar = false
    isUseUberJar = true
    isDaemon = false
}
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    //Java plugin
    id("java-library")

    //Fairy framework plugin
    id("io.fairyproject") version "0.8b1-SNAPSHOT"

    // Dependency management plugin
    id("io.spring.dependency-management") version "1.1.0"

    //Shadow plugin, provides the ability to shade fairy and other dependencies to compiled jar
    id("com.github.johnrengelman.shadow") version "8.1.1"

    kotlin("jvm") version "2.2.0"
}

val libPlugin = properties("lib.plugin").toBoolean()

group = properties("group")
version = properties("version")

// Fairy configuration
fairy {
    name.set(properties("name"))
    // Main Package
    mainPackage.set(properties("package"))
    if (libPlugin) {
        fairyPackage.set("io.fairyproject")
    } else {
        // Fairy Package
        fairyPackage.set(properties("package") + ".fairy")
    }

    bukkitProperties().bukkitApi = "1.13"
}

runServer {
    version.set(properties("spigot.version"))
    javaVersion.set(JavaVersion.VERSION_21)
}

val fairy by if (libPlugin) {
    configurations.compileOnlyApi
} else {
    configurations.api
}

dependencies {
    if (libPlugin) {
        compileOnlyApi("io.fairyproject:bukkit-platform")
        api("io.fairyproject:bukkit-bootstrap")
    } else {
        api("io.fairyproject:bukkit-bundles")
    }
    fairy("io.fairyproject:mc-animation")
    fairy("io.fairyproject:bukkit-command")
    fairy("io.fairyproject:bukkit-gui")
    fairy("io.fairyproject:mc-hologram")
    fairy("io.fairyproject:core-config")
    fairy("io.fairyproject:bukkit-xseries")
    fairy("io.fairyproject:bukkit-items")
    fairy("io.fairyproject:mc-nametag")
    fairy("io.fairyproject:mc-sidebar")
    fairy("io.fairyproject:bukkit-visibility")
    fairy("io.fairyproject:bukkit-visual")
    fairy("io.fairyproject:bukkit-timer")
    fairy("io.fairyproject:bukkit-nbt")
    fairy("io.fairyproject:mc-tablist")
    implementation(kotlin("stdlib-jdk8"))
}

// Repositories
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven(url = uri("https://oss.sonatype.org/content/repositories/snapshots/"))
    maven(url = uri("https://repo.codemc.io/repository/maven-public/"))
    // Spigot's repository for spigot api dependency
    maven(url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/"))
    maven(url = uri("https://repo.imanity.dev/imanity-libraries"))
}

// Dependencies
dependencies {
    compileOnly("org.imanity.imanityspigot:api:2025.3.3")
    compileOnly("org.spigotmc:spigot-api:${properties("spigot.version")}-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.reactivex.rxjava3:rxjava:3.0.6")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("io.papermc:paperlib:1.0.7")
    implementation("me.devnatan:inventory-framework-platform-bukkit:3.3.9")

    api("io.fairyproject:core-config")
}

tasks.withType(ShadowJar::class.java) {
    // Relocate fairy to avoid plugin conflict
    if (libPlugin) {
        relocate("io.fairyproject.bootstrap", "${properties("package")}.fairy.bootstrap")

        relocate("net.kyori", "io.fairyproject.libs.kyori")
        relocate("com.cryptomorin.xseries", "io.fairyproject.libs.xseries")
        relocate("org.yaml.snakeyaml", "io.fairyproject.libs.snakeyaml")
        relocate("com.google.gson", "io.fairyproject.libs.gson")
        relocate("com.github.retrooper.packetevents", "io.fairyproject.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "io.fairyproject.libs.packetevents")
    } else {
        val fairyPackage = properties("package") + ".fairy"
        relocate("io.fairyproject", fairyPackage)

        relocate("net.kyori", "$fairyPackage.libs.kyori")
        relocate("com.cryptomorin.xseries", "$fairyPackage.libs.xseries")
        relocate("org.yaml.snakeyaml", "$fairyPackage.libs.snakeyaml")
        relocate("com.google.gson", "$fairyPackage.libs.gson")
        relocate("com.github.retrooper.packetevents", "$fairyPackage.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "$fairyPackage.libs.packetevents")
    }
    relocate("io.fairyproject.bukkit.menu", "${properties("package")}.fairy.menu")
}
kotlin {
    jvmToolchain(21)
}
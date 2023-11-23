rootProject.name = "kotlin-debug-adapter"

include("adapter")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version "$kotlinVersion" apply false
        id("com.jaredsburrows.license") version "0.8.42" apply false
    }
}

sourceControl {
    gitRepository(java.net.URI.create("https://github.com/fwcd/kotlin-language-server.git")) {
        producesModule("kotlin-language-server:shared")
    }
}

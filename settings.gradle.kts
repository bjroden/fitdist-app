pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // TODO: Replace github source once library API is stabilized
    sourceControl {
        gitRepository(uri("https://github.com/bjroden/fitdist-kotlin.git")) {
            producesModule("org.example:fitdist-kotlin")
        }
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

rootProject.name = "fitdist-app"


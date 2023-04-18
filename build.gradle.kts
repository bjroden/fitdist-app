import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.8.20"
    id("org.jetbrains.compose") version "1.4.0"
    kotlin("plugin.serialization") version "1.8.20"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.material3:material3-desktop:1.4.0")
                implementation("org.jetbrains.compose.components:components-splitpane-desktop:1.4.0")
                implementation("org.jetbrains.lets-plot:lets-plot-batik:3.1.0")
                implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.3.0")
                implementation("org.jetbrains.kotlinx:dataframe:0.9.1")
                implementation("org.example:fitdist-kotlin:v0.1.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "fitdist-app"
            packageVersion = "1.0.0"
        }
    }
}

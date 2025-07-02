import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    js(IR) {
        browser()
        nodejs()
        binaries.executable()
    }
    jvm()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")

            dependencies {
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("io.github.yafrl:yafrl-core:0.4-SNAPSHOT")
                implementation("io.github.yafrl:yafrl-compose:0.4-SNAPSHOT")
                implementation("io.arrow-kt:arrow-core:2.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-framework-engine:6.0.0.M4")
                implementation("io.github.yafrl:yafrl-testing:0.4-SNAPSHOT")
                implementation("io.kotest:kotest-assertions-core:6.0.0.M4")
                implementation("io.kotest:kotest-property:6.0.0.M4")
            }
        }
    }
}

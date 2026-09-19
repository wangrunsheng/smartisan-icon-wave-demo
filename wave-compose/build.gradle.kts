plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        namespace = "com.russell.wave.compose"
        compileSdk = 36
        minSdk = 26
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    jvmToolchain(17)
    sourceSets.jvmTest.dependencies {
        implementation(kotlin("test"))
        implementation(compose.desktop.currentOs)
    }
    sourceSets.commonMain.dependencies {
        api(project(":wave-core"))
        api("org.jetbrains.compose.foundation:foundation:1.10.3")
        api("org.jetbrains.compose.runtime:runtime:1.10.3")
        api("org.jetbrains.compose.ui:ui:1.10.3")
    }
}

// Pixel tests use an offscreen Skia surface and do not need a desktop window.
tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
}

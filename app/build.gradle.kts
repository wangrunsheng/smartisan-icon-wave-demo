plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.russell.wavedemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.russell.wavedemo"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "3.1-compose"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":wave-view"))
    implementation(project(":wave-compose"))
    implementation("androidx.activity:activity-compose:1.12.4")
    testImplementation("junit:junit:4.13.2")
}

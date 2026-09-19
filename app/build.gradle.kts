plugins {
    id("com.android.application")
}

android {
    namespace = "com.russell.wavedemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.russell.wavedemo"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "3.0-library"
    }

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
    testImplementation("junit:junit:4.13.2")
}

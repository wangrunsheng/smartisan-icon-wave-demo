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
        versionCode = 9
        versionName = "2.5-sine"
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
    testImplementation("junit:junit:4.13.2")
}

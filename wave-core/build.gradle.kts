plugins { id("org.jetbrains.kotlin.multiplatform") }

kotlin {
    jvm()
    jvmToolchain(17)
    sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
}

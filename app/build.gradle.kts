plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.lemehelper"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.lemehelper"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3"
    }
}
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}

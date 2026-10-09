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
        versionCode = 4
        versionName = "0.4"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}

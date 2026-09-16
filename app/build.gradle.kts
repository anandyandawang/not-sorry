plugins {
    id("com.android.application")
}

android {
    namespace = "com.notsorry"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.notsorry"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
}

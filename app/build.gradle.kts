plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.notesapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notesapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Navigation component
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")  // Keep only the latest version
    implementation("com.google.android.material:material:1.11.0")  // Keep only the latest version
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.appcompat)
    // Remove this line since you're already explicitly defining material above
    // implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.cardview)
    implementation(libs.photoView)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.sdp)
    implementation(libs.ssp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
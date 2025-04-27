plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.japanesenaturalnesscheck"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.japanesenaturalnesscheck"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Retrofit for API calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp logging interceptor for debugging API calls
    implementation(libs.logging.interceptor)

    // Gson for JSON parsing
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Security for encrypted shared preferences
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

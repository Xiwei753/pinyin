plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.xiwei753.pinyin.t9"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.xiwei753.pinyin.t9"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Keep it minimal
    implementation("androidx.core:core-ktx:1.12.0")
    // No complex dependencies as requested

    testImplementation("junit:junit:4.13.2")
}

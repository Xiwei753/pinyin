import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val t9AssetsDir = layout.buildDirectory.dir("generated/t9Assets")

val ciVersionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val fallbackVersionCode = localProperties.getProperty("versionCode")?.toIntOrNull() ?: 1
val computedVersionCode = ciVersionCode ?: fallbackVersionCode

println("Computed versionCode: $computedVersionCode (CI GITHUB_RUN_NUMBER: $ciVersionCode, local fallback: $fallbackVersionCode)")

android {
    namespace = "io.github.xiwei753.pinyin.t9"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.xiwei753.pinyin.t9"
        minSdk = 23
        targetSdk = 34
        versionCode = computedVersionCode
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore/xiwei-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", t9AssetsDir)
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

val generateT9DictionaryAssets by tasks.registering(Exec::class) {
    description = "Generate t9_source_dict.tsv and t9_dict.db from Rime dictionary sources"
    workingDir = file("../../../../../")
    commandLine("python3", "tools/dictionary/build_t9_assets.py", "--out-dir", t9AssetsDir.get().asFile.absolutePath)

    inputs.dir("../../../../../third_party/rime-ice/cn_dicts")
    inputs.dir("../../../../../tools/dictionary")
    outputs.dir(t9AssetsDir)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
    dependsOn(generateT9DictionaryAssets)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(generateT9DictionaryAssets)
}

dependencies {
    testImplementation("org.robolectric:robolectric:4.11.1")
    implementation("androidx.core:core-ktx:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.xerial:sqlite-jdbc:3.41.2.1")
}

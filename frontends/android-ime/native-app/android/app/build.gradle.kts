val generateT9DictionaryAssets by tasks.registering(Exec::class) {
    description = "Generate t9_source_dict.tsv and t9_dict.db from Rime dictionary sources"
    workingDir = file("../../../../../")
    val outDir = file("${project.buildDir}/generated/t9Assets")
    commandLine("python3", "tools/dictionary/build_t9_assets.py", "--out-dir", outDir.absolutePath)

    inputs.dir("../../../../../third_party/rime-ice/cn_dicts")
    inputs.dir("../../../../../tools/dictionary")
    outputs.dir(outDir)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
    dependsOn(generateT9DictionaryAssets)
}

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

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "${project.buildDir}/generated/t9Assets")
        }
    }
}

// Ensure asset merging tasks depend on dictionary generation
tasks.whenTaskAdded {
    if (name.startsWith("merge") && name.endsWith("Assets")) {
        dependsOn(generateT9DictionaryAssets)
    }
}

dependencies {
    // Keep it minimal
    implementation("androidx.core:core-ktx:1.12.0")
    // No complex dependencies as requested

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.xerial:sqlite-jdbc:3.41.2.1")
}

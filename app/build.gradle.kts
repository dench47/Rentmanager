plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

fun gitCommitCount(): Int {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .directory(rootProject.projectDir)
        .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    val exit = process.waitFor()
    return if (exit == 0) output.toIntOrNull() ?: 1 else 1
}

val autoVersionCode = gitCommitCount()
val autoVersionName = "1.0.$autoVersionCode"

android {
    namespace = "com.rentmanager.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rentmanager.app"
        minSdk = 26
        targetSdk = 35
        versionCode = autoVersionCode
        versionName = autoVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://менеджераренды.рф/api/v1/\"")
    }

    signingConfigs {
        create("demo") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            val localIp = project.findProperty("local.server.ip")?.toString() ?: "192.168.0.152"
            buildConfigField("String", "API_BASE_URL", "\"http://$localIp:8080/api/v1/\"")
        }
        release {
            signingConfig = signingConfigs.getByName("demo")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // DataStore — сохранение черновика создания объекта между запусками
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // OSM-карты (osmdroid) — бесплатно, без ключей
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // EXIF-ориентация фото при сжатии перед загрузкой
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// ======== Deployment helpers ========

tasks.register("generateVersionJson") {
    group = "deploy"
    description = "Generates version.json for server-side version check (use after release build)"
    doLast {
        val apkUrl = "https://менеджераренды.рф/downloads/app-release.apk"
        val json = """
            {
              "version_code": $autoVersionCode,
              "version_name": "$autoVersionName",
              "min_client_version": $autoVersionCode,
              "apk_url": "$apkUrl",
              "force_update": false,
              "release_notes": ""
            }
        """.trimIndent()
        val outputDir = layout.buildDirectory.dir("outputs").get().asFile
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = outputDir.resolve("version.json")
        file.writeText(json)
        println(">> version.json generated: ${file.absolutePath}")
        println(">> version_code: $autoVersionCode, version_name: $autoVersionName")
    }
}

// Хук: после сборки release APK авто-генерируем version.json
tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }.configureEach {
    finalizedBy("generateVersionJson")
}
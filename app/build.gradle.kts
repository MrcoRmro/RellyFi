plugins {
    id("com.android.application") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("org.jetbrains.kotlin.kapt") version "1.9.0"
}

android {
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    namespace = "com.sdm.music"
    compileSdk = 34

    defaultConfig {
        ndk {
            abiFilters.add("arm64-v8a")
        }
        applicationId = "com.sdm.music"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "V2.1"
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
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // AndroidX Media3 (ExoPlayer + MediaSessionService + UI)
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // ViewPager2 y DocumentFile para Storage Access Framework
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // OkHttp para consultas API (LRCLIB lyrics)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

android.applicationVariants.configureEach {
    outputs.all {
        val outputImpl = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
        outputImpl.outputFileName = "REllyFi ${versionName}.apk"
    }
}

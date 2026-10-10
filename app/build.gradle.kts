import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {

    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.shephard.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.shephard.player"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "7.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // miuix-nav-android 0.9.4 is compiled for JVM 21 (its inline functions cannot be inlined into JVM 17 code).
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            )
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.compose.material3:material3:1.5.0-alpha26")

    implementation("com.materialkolor:material-kolor:5.0.0")

    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.3")

    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-shader-android:0.9.3")

    implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.3")
    // Navigation engine used by InstallerX Revived (NavDisplay, NavTransition, predictive back).
    // miuix-nav 0.9.4 needs no miuix-ui code; its squircle dependency is excluded so the
    // squircle version stays the one already used by the 0.9.3 UI artifacts.
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:0.9.4") {
        exclude(group = "top.yukonga.miuix.kmp", module = "miuix-squircle-android")
    }

    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    implementation("sh.calvin.reorderable:reorderable:3.0.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.guava:guava:33.2.1-android")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("net.jthink:jaudiotagger:3.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

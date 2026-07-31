import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 ships built-in Kotlin support, so the standalone
    // `org.jetbrains.kotlin.android` plugin must not be applied any more.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.shephard.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.shephard.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "5.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }



    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                // miuix-blur's public API uses Kotlin context parameters.
                "-Xcontext-parameters"
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
    // KRİTİK — Miuix 0.9.3 içindeki her bottom sheet / dialog / popup,
    // `androidx.navigationevent.compose.NavigationBackHandler` kullanıyor. Bu API
    // `LocalNavigationEventDispatcherOwner`'ın sağlanmış olmasını ZORUNLU tutar ve o owner'ı
    // ancak androidx.activity 1.12+ (ComponentActivity) sağlar. Proje daha önce
    // activity-compose 1.9.1 kullanıyordu; bu yüzden HERHANGİ bir drawer (queue, lyrics,
    // edit music, edit playlist, dil seçimi...) açılmaya çalışıldığında
    // `IllegalStateException: No NavigationEventDispatcherOwner was provided` fırlatılıyor
    // ve uygulama çöküyordu. Miuix'in ve InstallerX'in kendi kullandığı sürümlere
    // hizalıyoruz.
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")

    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    // Miuix / ComposeX stack — UI components + real Liquid Glass backdrop pipeline.
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.3")
    // Miuix'in kendi ikon seti — Cover editleme drawer'larındaki ✓ (Ok) ve × (Close)
    // işaretleri buradan geliyor (InstallerX de aynı artefaktı kullanıyor).
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-shader-android:0.9.3")
    implementation("androidx.navigation3:navigation3-runtime:1.1.4")
    implementation("androidx.collection:collection:1.6.0")

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
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Tag editor for MP3 / M4A offline editing
    implementation("net.jthink:jaudiotagger:3.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

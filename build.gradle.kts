// AGP 9 ships built-in Kotlin support, so there is no `org.jetbrains.kotlin.android` plugin
// to version. The Kotlin Gradle Plugin it uses is upgraded through the buildscript classpath;
// this pins it to Kotlin 2.4, matching the Compose compiler plugin below (they must be the
// same version).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}

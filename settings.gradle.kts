pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Miuix NavDisplay snapshot'ı (0.9.3 release'da NavDisplay yok; InstallerX ile aynı SNAPSHOT).
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
}
rootProject.name = "Lambda Player"
include(":app")

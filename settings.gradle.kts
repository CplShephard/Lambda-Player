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
        maven { setUrl("https://jitpack.io") }
        // GitHub Packages (compose-miuix-ui/miuix) — Miuix NavDisplay SNAPSHOT'ı buradan.
        // NOT: GitHub Packages anonim erişimi desteklemez; CI'da GITHUB_ACTOR/GITHUB_TOKEN
        // otomatik olarak mevcut (InstallerX-Revived ile birebir aynı kurulum).
        val gprUser = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        val gprKey = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
        maven {
            name = "GitHubPackagesMiuix"
            url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
            if (gprUser.isPresent && gprKey.isPresent) {
                credentials {
                    username = gprUser.get()
                    password = gprKey.get()
                }
            }
        }
        mavenLocal()
    }
}
rootProject.name = "Lambda Player"
include(":app")

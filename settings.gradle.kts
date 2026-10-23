pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://www.jitpack.io")
        google()
        mavenCentral()
    }
}

rootProject.name = "MultiCloner"

include(":app")
// Motorul de virtualizare BlackBox (din ALEX5402/NewBlackbox, Apache 2.0)
include(":Bcore")
include(":black-reflection")
include(":compiler")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hear2ReadNG Android"
include(":app")
include(":hi")
include(":en")
include(":kn")
include(":as")
include(":gu")
include(":ml")
include(":mr")
include(":ne")
include(":or")
include(":pa")
include(":si")
include(":ta")
include(":te")
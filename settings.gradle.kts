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
include(":en")
//include(":kn")
//include(":hi")
include(":bn")
include(":kn")

//val enableAllVoices: String? by settings
//if (enableAllVoices == "true") {
val enableAllVoices = providers.gradleProperty("enableAllVoices")
if (enableAllVoices.getOrNull() == "true") {
    include(":as", ":gu", ":ml", ":mr", ":ne", ":or", ":pa", ":si", ":ta", ":te",
        ":hi"
//        ":sa", ":bn"
    )
}
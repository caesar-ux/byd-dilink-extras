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
    }
}

rootProject.name = "byd-dilink-extras"

include(":core:ui")
include(":core:data")
include(":app-hazard")
include(":app-fuelcost")
include(":app-tireguard")
include(":app-prayer")

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

rootProject.name = "FitPlannerAI"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:planner")
include(":feature:exercises")
include(":feature:workout")
include(":feature:progress")
include(":feature:profile")

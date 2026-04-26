pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            name = "PaperMC"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }
}

include("daisy-companion-plugin")
project(":daisy-companion-plugin").projectDir = file("mods/daisy-companion-plugin")

rootProject.name = "DaisyMinecraft"

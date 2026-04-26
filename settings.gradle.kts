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
        maven {
            name = "DaisyCloudGitHubPackages"
            url = uri("https://maven.pkg.github.com/DaisyQuest/DaisyCloud")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orElse(providers.environmentVariable("GITHUB_USERNAME"))
                    .orElse("not-set")
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orElse("not-set")
                    .get()
            }
            content {
                includeGroup("dev.daisycloud")
            }
        }
    }
}

include("daisy-companion-plugin")
project(":daisy-companion-plugin").projectDir = file("mods/daisy-companion-plugin")

rootProject.name = "DaisyMinecraft"

val daisyCloudComposite = file(
    providers.gradleProperty("daisyminecraft.daisyCloudCompositePath")
        .orElse(providers.environmentVariable("DAISYMINECRAFT_DAISYCLOUD_COMPOSITE_PATH"))
        .orElse("../DaisyCloud")
        .get()
)
val useDaisyCloudComposite = providers.gradleProperty("daisyminecraft.includeDaisyCloudComposite")
    .map(String::toBoolean)
    .orElse(daisyCloudComposite.isDirectory)
    .get()

if (useDaisyCloudComposite) {
    require(daisyCloudComposite.isDirectory) {
        "DaisyCloud composite build was requested but ${daisyCloudComposite.absolutePath} was not found."
    }
    includeBuild(daisyCloudComposite) {
        dependencySubstitution {
            substitute(module("dev.daisycloud:cloud-model")).using(project(":cloud-model"))
            substitute(module("dev.daisycloud:cloud-state")).using(project(":cloud-state"))
            substitute(module("dev.daisycloud:cloud-provider-spi")).using(project(":cloud-provider-spi"))
            substitute(module("dev.daisycloud:cloud-provider-daisybase")).using(project(":cloud-provider-daisybase"))
        }
    }
}

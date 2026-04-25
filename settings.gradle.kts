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
            name = "DaisyCloudGitHubPackages"
            url = uri("https://maven.pkg.github.com/DaisyQuest/DaisyCloud")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orElse(providers.environmentVariable("GITHUB_USERNAME"))
                    .orElse("not-set")
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))
                    .orElse("not-set")
                    .get()
            }
            content {
                includeGroup("dev.daisycloud")
            }
        }
    }
}

rootProject.name = "DaisyMinecraft"

val daisyCloudComposite = file("../DaisyCloud")
val useDaisyCloudComposite = providers.gradleProperty("daisyminecraft.includeDaisyCloudComposite")
    .map(String::toBoolean)
    .orElse(daisyCloudComposite.isDirectory)
    .get()

if (useDaisyCloudComposite) {
    require(daisyCloudComposite.isDirectory) {
        "DaisyCloud composite build was requested but ../DaisyCloud was not found."
    }
    includeBuild(daisyCloudComposite) {
        dependencySubstitution {
            substitute(module("dev.daisycloud:cloud-model")).using(project(":cloud-model"))
            substitute(module("dev.daisycloud:cloud-provider-spi")).using(project(":cloud-provider-spi"))
            substitute(module("dev.daisycloud:cloud-provider-daisybase")).using(project(":cloud-provider-daisybase"))
        }
    }
}

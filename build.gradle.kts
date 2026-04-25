import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
    `maven-publish`
}

group = "dev.daisycloud"
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

dependencies {
    implementation("dev.daisycloud:cloud-model:$version")

    api("dev.daisycloud:cloud-provider-spi:$version")
    api("dev.daisycloud:cloud-provider-daisybase:$version")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "daisy-minecraft"
            from(components["java"])
            pom {
                name.set("DaisyMinecraft")
                description.set("Minecraft server hosting provider for DaisyCloud.")
                url.set("https://github.com/DaisyQuest/DaisyMinecraft")
            }
        }
    }
}

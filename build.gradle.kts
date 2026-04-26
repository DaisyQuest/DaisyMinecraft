import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar

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

evaluationDependsOn(":daisy-companion-plugin")

val copyBundledAddons = tasks.register<Copy>("copyBundledAddons") {
    val companionJar = project(":daisy-companion-plugin").tasks.named<Jar>("jar").flatMap { it.archiveFile }
    dependsOn(":daisy-companion-plugin:jar")
    from(companionJar)
    into(layout.buildDirectory.dir("generated-resources/bundled-addons/daisyminecraft/bundled-plugins"))
    rename { "DaisyCompanion.jar" }
}

sourceSets {
    main {
        resources.srcDir(layout.buildDirectory.dir("generated-resources/bundled-addons"))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named("processResources") {
    dependsOn(copyBundledAddons)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(copyBundledAddons)
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

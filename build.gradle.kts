import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import java.io.File

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

val daisyCloudArtifactsDir: File? = providers.gradleProperty("daisyminecraft.daisyCloudArtifactsDir")
    .orElse(providers.environmentVariable("DAISYMINECRAFT_DAISYCLOUD_ARTIFACTS_DIR"))
    .map { file(it) }
    .orNull
val useDaisyCloudArtifacts = providers.gradleProperty("daisyminecraft.useDaisyCloudArtifacts")
    .orElse(providers.environmentVariable("DAISYMINECRAFT_USE_DAISYCLOUD_ARTIFACTS"))
    .map(String::toBoolean)
    .orElse(daisyCloudArtifactsDir?.isDirectory == true)
    .get()

fun daisyCloudArtifact(moduleName: String): File {
    val directory = requireNotNull(daisyCloudArtifactsDir) {
        "DaisyCloud artifact mode requires -Pdaisyminecraft.daisyCloudArtifactsDir or DAISYMINECRAFT_DAISYCLOUD_ARTIFACTS_DIR."
    }
    require(directory.isDirectory) {
        "DaisyCloud artifacts directory does not exist: ${directory.absolutePath}"
    }
    val exactName = "$moduleName-$version.jar"
    val matches = fileTree(directory) {
        include("**/$exactName")
        include("**/$moduleName-*.jar")
        exclude("**/*-sources.jar")
        exclude("**/*-javadoc.jar")
    }.files.sortedBy { it.absolutePath }
    val candidates = matches.filter { it.name == exactName }.ifEmpty { matches }
    require(candidates.size == 1) {
        "Expected exactly one $moduleName artifact in ${directory.absolutePath}, found ${candidates.size}: " +
            candidates.joinToString { it.absolutePath }
    }
    return candidates.single()
}

dependencies {
    if (useDaisyCloudArtifacts) {
        implementation(files(daisyCloudArtifact("cloud-model")))
        implementation(files(daisyCloudArtifact("cloud-state")))
        api(files(daisyCloudArtifact("cloud-provider-spi")))
        api(files(daisyCloudArtifact("cloud-provider-daisybase")))
    } else {
        implementation("dev.daisycloud:cloud-model:$version")
        implementation("dev.daisycloud:cloud-state:$version")
        api("dev.daisycloud:cloud-provider-spi:$version")
        api("dev.daisycloud:cloud-provider-daisybase:$version")
    }

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

package dev.daisycloud.provider.minecraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftContainerAssetsTest {
    private final Path containerDir = Path.of("src", "main", "container");

    @Test
    void dockerfileDefinesRunnableMinecraftServerImage() throws IOException {
        String dockerfile = Files.readString(containerDir.resolve("Dockerfile"));

        assertTrue(dockerfile.contains("eclipse-temurin:21-jre"));
        assertTrue(dockerfile.contains("daisyminecraft-entrypoint"));
        assertTrue(dockerfile.contains("daisyminecraft-healthcheck"));
        assertTrue(dockerfile.contains("USER minecraft:minecraft"));
        assertTrue(dockerfile.contains("VOLUME [\"/data\"]"));
        assertTrue(dockerfile.contains("EXPOSE 25565/tcp"));
        assertTrue(dockerfile.contains("25565/udp"));
        assertTrue(dockerfile.contains("HEALTHCHECK"));
    }

    @Test
    void entrypointEnforcesEulaAndVerifiedJarSources() throws IOException {
        String entrypoint = Files.readString(containerDir.resolve("daisyminecraft-entrypoint.sh"));

        assertTrue(entrypoint.contains("EULA=TRUE"));
        assertTrue(entrypoint.contains("DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL"));
        assertTrue(entrypoint.contains("DAISY_MINECRAFT_CUSTOM_SERVER_JAR_SHA256"));
        assertTrue(entrypoint.contains("sha256sum -c"));
        assertTrue(entrypoint.contains("curl --fail"));
        assertTrue(entrypoint.contains("eula=true"));
        assertTrue(entrypoint.contains("DAISY_MINECRAFT_CUSTOM_SERVER_COMMAND"));
        assertTrue(entrypoint.contains("-jar \"$SERVER_JAR_NAME\" nogui"));
    }

    @Test
    void healthcheckVerifiesPortOrProcess() throws IOException {
        String healthcheck = Files.readString(containerDir.resolve("daisyminecraft-healthcheck.sh"));

        assertTrue(healthcheck.contains("DAISY_MINECRAFT_PORT"));
        assertTrue(healthcheck.contains("nc -z"));
        assertTrue(healthcheck.contains("pgrep -f"));
    }
}

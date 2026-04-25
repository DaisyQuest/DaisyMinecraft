package dev.daisycloud.provider.minecraft;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftLocalRuntimeDriverTest {
    @TempDir
    Path temp;

    @Test
    void materializesWorkspaceButRefusesToStartWithoutConfiguredServerJar() throws Exception {
        Path runtimeRoot = temp.resolve("runtime");
        Path missingJar = temp.resolve("artifacts").resolve("server.jar");
        MinecraftLocalRuntimeDriver driver = new MinecraftLocalRuntimeDriver(runtimeRoot, missingJar, "java");

        driver.pullImage("daisycloud/minecraft-paper:1.21.11");
        driver.ensureVolume("daisycloud-mc-test", "/data");
        driver.writeStartupFile("mc-test", "server.properties", "server-port=25565\nmax-players=20\n");
        driver.bindPort("mc-test", "25565/tcp", "65534");

        MinecraftRuntimeContainerSpec spec = new MinecraftRuntimeContainerSpec(
                "mc-test",
                "daisycloud/minecraft-paper:1.21.11",
                "java -jar server.jar nogui",
                Map.of("EULA", "TRUE"),
                Map.of("25565/tcp", "65534"),
                Map.of("daisycloud-mc-test", "/data"),
                Map.of("memoryMb", "512"),
                "unless-stopped",
                Map.of("app", "minecraft"),
                "primary");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> driver.startContainer(spec));

        assertTrue(error.getMessage().contains("Minecraft server jar not found"));
        assertTrue(Files.readString(runtimeRoot.resolve("last-requested-image.txt"))
                .contains("daisycloud/minecraft-paper:1.21.11"));
        assertTrue(Files.readString(runtimeRoot.resolve("volumes").resolve("daisycloud-mc-test").resolve("server.properties"))
                .contains("max-players=20"));
        assertTrue(Files.exists(runtimeRoot.resolve("services").resolve("mc-test").resolve("container-spec.properties")));
        assertEquals("missing-server-jar", driver.probeHealth("mc-test", Map.of("process", "running")).get("process"));
        assertEquals("not-reachable", driver.probeHealth("mc-test", Map.of("port", "reachable")).get("port"));
    }

    @Test
    void rejectsUnsafeNamesAndStartupFilePaths() {
        MinecraftLocalRuntimeDriver driver = new MinecraftLocalRuntimeDriver(
                temp.resolve("runtime"),
                temp.resolve("server.jar"),
                "java");

        assertThrows(IllegalArgumentException.class, () -> driver.ensureVolume("../bad", "/data"));
        assertThrows(IllegalArgumentException.class, () -> driver.writeStartupFile("mc-test", "../server.properties", "bad"));
        assertThrows(IllegalArgumentException.class, () -> driver.bindPort("mc test", "25565/tcp", "25565"));
    }
}

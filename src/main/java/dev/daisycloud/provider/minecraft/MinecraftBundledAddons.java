package dev.daisycloud.provider.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class MinecraftBundledAddons {
    public static final String DAISY_COMPANION_ID = "daisyminecraft:daisy-companion";
    public static final String DAISY_COMPANION_PLUGIN_PATH = "plugins/DaisyCompanion.jar";
    public static final String DAISY_COMPANION_CONFIG_PATH = "plugins/DaisyCompanion/config.yml";
    public static final String DAISY_COMPANION_RESOURCE_PATH =
            "daisyminecraft/bundled-plugins/DaisyCompanion.jar";

    private static final Set<String> PLUGIN_SERVER_TYPES = Set.of("paper", "spigot", "purpur");
    private static final String DOG_NAME = "Daisy";
    private static final double HEALTH_MULTIPLIER = 100.0D;
    private static final double SCALE_MULTIPLIER = 2.0D;
    private static final long SPAWN_DELAY_TICKS = 20L;

    private MinecraftBundledAddons() {
    }

    public static String defaultDaisyCompanionMode(String serverType) {
        return PLUGIN_SERVER_TYPES.contains(requireText(serverType, "serverType")) ? "enabled" : "disabled";
    }

    public static void validateDaisyCompanion(String mode, String serverType) {
        validateDaisyCompanion(mode, serverType, "none");
    }

    public static void validateDaisyCompanion(String mode, String serverType, String customContentSupport) {
        boolean pluginCompatible = PLUGIN_SERVER_TYPES.contains(requireText(serverType, "serverType"))
                || ("custom".equals(serverType)
                && Set.of("plugins", "mods-and-plugins").contains(requireText(customContentSupport, "customContentSupport")));
        if ("enabled".equals(requireText(mode, "daisyCompanion"))
                && !pluginCompatible) {
            if (!"custom".equals(serverType)) {
                throw new IllegalArgumentException("daisyCompanion=enabled requires serverType paper, spigot, or purpur");
            }
            throw new IllegalArgumentException("daisyCompanion=enabled requires a plugin-compatible server type");
        }
    }

    public static Map<String, String> fromPlannedAttributes(Map<String, String> planned) {
        Map<String, String> attributes = new LinkedHashMap<>();
        String mode = require(planned, "daisyCompanion");
        attributes.put("bundledAddonSchema", "daisyminecraft.bundled-addons.v1");
        attributes.put("daisyCompanion", mode);
        if (!"enabled".equals(mode)) {
            attributes.put("bundledAddonCount", "0");
            attributes.put("bundledAddonIds", "");
            attributes.put("bundledPlugins", "");
            attributes.put("bundledAddonPlan", "disabled");
            attributes.put("bundledAddonEvidence", "daisy-companion disabled for serverType=" + require(planned, "serverType"));
            attributes.put("daisyCompanionPluginPath", "");
            attributes.put("daisyCompanionConfigPath", "");
            attributes.put("daisyCompanionResourcePath", "");
            attributes.put("daisyCompanionSha256", "");
            attributes.put("daisyCompanionDogName", "");
            attributes.put("daisyCompanionHealthMultiplier", "");
            attributes.put("daisyCompanionScaleMultiplier", "");
            return Map.copyOf(attributes);
        }

        String sha256 = classpathResourceSha256().orElse("pending-build-resource");
        attributes.put("bundledAddonCount", "1");
        attributes.put("bundledAddonIds", DAISY_COMPANION_ID);
        attributes.put("bundledPlugins", "daisy-companion=" + DAISY_COMPANION_PLUGIN_PATH);
        attributes.put("bundledAddonPlan", "install:" + DAISY_COMPANION_ID + "=>" + DAISY_COMPANION_PLUGIN_PATH);
        attributes.put("bundledAddonEvidence",
                "first-party Paper plugin;first-join dog spawn;owner-bound wolf;healthMultiplier=100;scaleMultiplier=2");
        attributes.put("daisyCompanionPluginPath", DAISY_COMPANION_PLUGIN_PATH);
        attributes.put("daisyCompanionConfigPath", DAISY_COMPANION_CONFIG_PATH);
        attributes.put("daisyCompanionResourcePath", DAISY_COMPANION_RESOURCE_PATH);
        attributes.put("daisyCompanionSha256", sha256);
        attributes.put("daisyCompanionDogName", DOG_NAME);
        attributes.put("daisyCompanionHealthMultiplier", Double.toString(HEALTH_MULTIPLIER));
        attributes.put("daisyCompanionScaleMultiplier", Double.toString(SCALE_MULTIPLIER));
        attributes.put("daisyCompanionSpawnDelayTicks", Long.toString(SPAWN_DELAY_TICKS));
        return Map.copyOf(attributes);
    }

    public static Map<String, String> startupFiles(Map<String, String> planned) {
        if (!"enabled".equals(planned.getOrDefault("daisyCompanion", "disabled"))) {
            return Map.of();
        }
        Map<String, String> files = new LinkedHashMap<>();
        files.put(DAISY_COMPANION_CONFIG_PATH, daisyCompanionConfig());
        files.put("daisyminecraft-bundled-addons.json", bundledAddonsJson(planned));
        return Map.copyOf(files);
    }

    public static String installBundledAddons(
            MinecraftRuntimeDriver runtimeDriver,
            String serviceName,
            Map<String, String> attributes) {
        Objects.requireNonNull(runtimeDriver, "runtimeDriver must not be null");
        if (!"enabled".equals(attributes.getOrDefault("daisyCompanion", "disabled"))) {
            return "skipped:disabled";
        }
        byte[] jar = classpathResourceBytes()
                .orElseThrow(() -> new IllegalStateException("Bundled DaisyCompanion plugin resource not found: "
                        + DAISY_COMPANION_RESOURCE_PATH));
        runtimeDriver.writeBinaryFile(serviceName, DAISY_COMPANION_PLUGIN_PATH, jar);
        return DAISY_COMPANION_ID + "=>" + DAISY_COMPANION_PLUGIN_PATH
                + ";bytes=" + jar.length
                + ";sha256=" + sha256(jar);
    }

    private static String daisyCompanionConfig() {
        return "dog-name: " + DOG_NAME + "\n"
                + "health-multiplier: " + HEALTH_MULTIPLIER + "\n"
                + "scale-multiplier: " + SCALE_MULTIPLIER + "\n"
                + "spawn-delay-ticks: " + SPAWN_DELAY_TICKS + "\n"
                + "only-first-join: true\n"
                + "collar-color: YELLOW\n";
    }

    private static String bundledAddonsJson(Map<String, String> planned) {
        return "{\n"
                + "  \"schemaVersion\": \"daisyminecraft.bundled-addons.v1\",\n"
                + "  \"addons\": [\n"
                + "    {\n"
                + "      \"id\": \"" + json(DAISY_COMPANION_ID) + "\",\n"
                + "      \"kind\": \"paper-plugin\",\n"
                + "      \"pluginPath\": \"" + json(DAISY_COMPANION_PLUGIN_PATH) + "\",\n"
                + "      \"configPath\": \"" + json(DAISY_COMPANION_CONFIG_PATH) + "\",\n"
                + "      \"resourcePath\": \"" + json(DAISY_COMPANION_RESOURCE_PATH) + "\",\n"
                + "      \"sha256\": \"" + json(planned.getOrDefault("daisyCompanionSha256", "")) + "\",\n"
                + "      \"dogName\": \"" + DOG_NAME + "\",\n"
                + "      \"healthMultiplier\": " + HEALTH_MULTIPLIER + ",\n"
                + "      \"scaleMultiplier\": " + SCALE_MULTIPLIER + "\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";
    }

    private static Optional<String> classpathResourceSha256() {
        return classpathResourceBytes().map(MinecraftBundledAddons::sha256);
    }

    private static Optional<byte[]> classpathResourceBytes() {
        try (InputStream stream = MinecraftBundledAddons.class.getClassLoader()
                .getResourceAsStream(DAISY_COMPANION_RESOURCE_PATH)) {
            if (stream == null) {
                return Optional.empty();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            stream.transferTo(output);
            return Optional.of(output.toByteArray());
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read bundled addon resource: " + error.getMessage(), error);
        }
    }

    private static String require(Map<String, String> attributes, String name) {
        return requireText(attributes.get(name), name);
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }

    private static String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 digest is unavailable", error);
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}

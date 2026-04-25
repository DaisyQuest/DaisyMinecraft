package dev.daisycloud.provider.minecraft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record MinecraftContentLock(
        String schemaVersion,
        String minecraftVersion,
        String serverType,
        String modLoader,
        String defaultSource,
        List<MinecraftContentLockItem> items,
        List<String> warnings,
        String resolverMode,
        String digest) {
    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");

    public MinecraftContentLock {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        minecraftVersion = requireText(minecraftVersion, "minecraftVersion");
        serverType = requireText(serverType, "serverType");
        modLoader = requireText(modLoader, "modLoader");
        defaultSource = requireText(defaultSource, "defaultSource");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        resolverMode = requireText(resolverMode, "resolverMode");
        digest = requireText(digest, "digest");
    }

    public static MinecraftContentLock fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String minecraftVersion = required(planned, "minecraftVersion");
        String serverType = required(planned, "serverType");
        String modLoader = required(planned, "modLoader");
        String defaultSource = required(planned, "modSource");
        List<MinecraftContentLockItem> requestedItems = requestedItems(planned, minecraftVersion, serverType, modLoader, defaultSource);
        MinecraftContentResolution resolution = MinecraftContentCatalog.defaultCatalog()
                .resolve(minecraftVersion, serverType, modLoader, requestedItems);
        String payload = canonicalPayload("daisyminecraft.content.v1",
                minecraftVersion,
                serverType,
                modLoader,
                defaultSource,
                resolution.items());
        return new MinecraftContentLock(
                "daisyminecraft.content.v1",
                minecraftVersion,
                serverType,
                modLoader,
                defaultSource,
                resolution.items(),
                resolution.warnings(),
                resolution.resolverMode(),
                sha256(payload));
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contentLockSchema", schemaVersion);
        attributes.put("contentLockDigest", digest);
        attributes.put("contentLockItemCount", Integer.toString(items.size()));
        attributes.put("contentLockState", "locked");
        attributes.put("contentResolverMode", resolverMode);
        attributes.put("contentLockWarningCount", Integer.toString(warnings.size()));
        attributes.put("contentWarnings", String.join(";", warnings));
        attributes.put("contentLockSummary", summary());
        return Map.copyOf(attributes);
    }

    public String toJson() {
        return "{\n"
                + "  \"schemaVersion\": \"" + json(schemaVersion) + "\",\n"
                + "  \"contentDigest\": \"" + json(digest) + "\",\n"
                + "  \"minecraftVersion\": \"" + json(minecraftVersion) + "\",\n"
                + "  \"serverType\": \"" + json(serverType) + "\",\n"
                + "  \"modLoader\": \"" + json(modLoader) + "\",\n"
                + "  \"defaultSource\": \"" + json(defaultSource) + "\",\n"
                + "  \"resolverMode\": \"" + json(resolverMode) + "\",\n"
                + "  \"items\": " + itemsJson(items) + ",\n"
                + "  \"warnings\": " + stringArrayJson(warnings) + "\n"
                + "}\n";
    }

    private String summary() {
        long modCount = items.stream().filter(item -> item.kind().equals("mod")).count();
        long pluginCount = items.stream().filter(item -> item.kind().equals("plugin")).count();
        long modpackCount = items.stream().filter(item -> item.kind().equals("modpack")).count();
        return "modpacks=" + modpackCount + ";mods=" + modCount + ";plugins=" + pluginCount + ";digest=" + digest;
    }

    private static List<MinecraftContentLockItem> requestedItems(
            Map<String, String> planned,
            String minecraftVersion,
            String serverType,
            String modLoader,
            String defaultSource) {
        java.util.ArrayList<MinecraftContentLockItem> items = new java.util.ArrayList<>();
        String modpackId = planned.getOrDefault("modpackId", "");
        if (!modpackId.isBlank()) {
            items.add(item("modpack", defaultSource, modpackId, serverType, minecraftVersion));
        }
        for (String selectedMod : csv(planned.getOrDefault("selectedMods", ""))) {
            SourceAndId sourceAndId = sourceAndId(selectedMod, defaultSource);
            items.add(item("mod", sourceAndId.source(), sourceAndId.id(), modLoader, minecraftVersion));
        }
        for (String selectedPlugin : csv(planned.getOrDefault("selectedPlugins", ""))) {
            SourceAndId sourceAndId = sourceAndId(selectedPlugin, "plugin-catalog");
            items.add(item("plugin", sourceAndId.source(), sourceAndId.id(), serverType, minecraftVersion));
        }
        return List.copyOf(items);
    }

    private static MinecraftContentLockItem item(
            String kind,
            String source,
            String id,
            String loader,
            String minecraftVersion) {
        return new MinecraftContentLockItem(kind, source, id, loader, minecraftVersion, "required", "");
    }

    private static String canonicalPayload(
            String schemaVersion,
            String minecraftVersion,
            String serverType,
            String modLoader,
            String defaultSource,
            List<MinecraftContentLockItem> items) {
        return schemaVersion + "\n"
                + minecraftVersion + "\n"
                + serverType + "\n"
                + modLoader + "\n"
                + defaultSource + "\n"
                + itemsJson(items);
    }

    private static String itemsJson(List<MinecraftContentLockItem> items) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < items.size(); index++) {
            MinecraftContentLockItem item = items.get(index);
            if (index > 0) {
                builder.append(", ");
            }
            builder.append("{")
                    .append("\"kind\":\"").append(json(item.kind())).append("\",")
                    .append("\"source\":\"").append(json(item.source())).append("\",")
                    .append("\"id\":\"").append(json(item.id())).append("\",")
                    .append("\"loader\":\"").append(json(item.loader())).append("\",")
                    .append("\"minecraftVersion\":\"").append(json(item.minecraftVersion())).append("\",")
                    .append("\"resolution\":\"").append(json(item.resolution())).append("\",")
                    .append("\"sha256\":\"").append(json(item.sha256())).append("\"")
                    .append("}");
        }
        return builder.append(']').toString();
    }

    private static String stringArrayJson(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append('"').append(json(values.get(index))).append('"');
        }
        return builder.append(']').toString();
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(candidate -> !candidate.isBlank())
                .toList();
    }

    private static SourceAndId sourceAndId(String raw, String defaultSource) {
        String value = requireSafe(raw, "content item");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return new SourceAndId(defaultSource, value);
        }
        return new SourceAndId(
                requireSafe(value.substring(0, separator), "content source"),
                requireSafe(value.substring(separator + 1), "content id"));
    }

    private static String required(Map<String, String> planned, String name) {
        return requireText(planned.get(name), name);
    }

    private static String requireSafe(String value, String name) {
        String text = requireText(value, name);
        if (!SAFE_TOKEN.matcher(text).matches()) {
            throw new IllegalArgumentException(name + " must be a safe token");
        }
        return text;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
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

    private record SourceAndId(String source, String id) {
    }
}

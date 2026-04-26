package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MinecraftContentCatalog {
    private final Map<String, Descriptor> descriptors;

    private MinecraftContentCatalog(List<Descriptor> descriptors) {
        Map<String, Descriptor> indexed = new LinkedHashMap<>();
        for (Descriptor descriptor : descriptors) {
            Descriptor value = Objects.requireNonNull(descriptor, "descriptors must not contain null");
            indexed.put(key(value.kind(), value.source(), value.id()), value);
        }
        this.descriptors = Map.copyOf(indexed);
    }

    public static MinecraftContentCatalog defaultCatalog() {
        return new MinecraftContentCatalog(List.of(
                mod("modrinth", "sodium", Set.of("fabric", "quilt")),
                mod("curseforge", "sodium", Set.of("fabric", "quilt")),
                mod("modrinth", "lithium", Set.of("fabric", "quilt")),
                mod("curseforge", "lithium", Set.of("fabric", "quilt")),
                mod("modrinth", "fabric-api", Set.of("fabric", "quilt")),
                mod("curseforge", "fabric-api", Set.of("fabric", "quilt")),
                new Descriptor(
                        "mod",
                        "modrinth",
                        "sodium-extra",
                        Set.of("fabric", "quilt"),
                        Set.of(),
                        Set.of("*"),
                        List.of(new Dependency("mod", "modrinth", "sodium")),
                        List.of()),
                plugin("plugin-catalog", "luckperms", Set.of("paper", "spigot", "purpur", "custom")),
                plugin("plugin-catalog", "geyser", Set.of("paper", "spigot", "purpur", "custom")),
                plugin("plugin-catalog", "floodgate", Set.of("paper", "spigot", "purpur", "custom")),
                plugin("spigotmc", "luckperms", Set.of("paper", "spigot", "purpur", "custom")),
                plugin("spigotmc", "geyser", Set.of("paper", "spigot", "purpur", "custom")),
                plugin("spigotmc", "floodgate", Set.of("paper", "spigot", "purpur", "custom"))));
    }

    public MinecraftContentResolution resolve(
            String minecraftVersion,
            String serverType,
            String modLoader,
            List<MinecraftContentLockItem> requestedItems) {
        Objects.requireNonNull(requestedItems, "requestedItems must not be null");
        Map<String, MinecraftContentLockItem> resolved = new LinkedHashMap<>();
        java.util.ArrayList<String> warnings = new java.util.ArrayList<>();

        for (MinecraftContentLockItem item : requestedItems) {
            resolveItem(item, minecraftVersion, serverType, modLoader, resolved, warnings);
        }

        return new MinecraftContentResolution(List.copyOf(resolved.values()), List.copyOf(warnings), "offline-metadata");
    }

    private void resolveItem(
            MinecraftContentLockItem item,
            String minecraftVersion,
            String serverType,
            String modLoader,
            Map<String, MinecraftContentLockItem> resolved,
            List<String> warnings) {
        String key = key(item.kind(), item.source(), item.id());
        if (resolved.containsKey(key)) {
            return;
        }

        Descriptor descriptor = descriptors.get(key);
        if (descriptor == null) {
            warnings.add("metadata unavailable for " + item.kind() + " " + item.source() + ":" + item.id());
            resolved.put(key, item);
            return;
        }

        descriptor.validate(minecraftVersion, serverType, modLoader);
        resolved.put(key, item);
        for (Dependency dependency : descriptor.dependencies()) {
            MinecraftContentLockItem dependencyItem = new MinecraftContentLockItem(
                    dependency.kind(),
                    dependency.source(),
                    dependency.id(),
                    dependency.kind().equals("plugin") ? serverType : modLoader,
                    minecraftVersion,
                    "required",
                    "");
            resolveItem(dependencyItem, minecraftVersion, serverType, modLoader, resolved, warnings);
        }
        warnings.addAll(descriptor.warnings());
    }

    private static Descriptor mod(String source, String id, Set<String> loaders) {
        return new Descriptor("mod", source, id, loaders, Set.of(), Set.of("*"), List.of(), List.of());
    }

    private static Descriptor plugin(String source, String id, Set<String> serverTypes) {
        return new Descriptor("plugin", source, id, Set.of(), serverTypes, Set.of("*"), List.of(), List.of());
    }

    private static String key(String kind, String source, String id) {
        return kind + "|" + source + "|" + id;
    }

    private record Descriptor(
            String kind,
            String source,
            String id,
            Set<String> supportedLoaders,
            Set<String> supportedServerTypes,
            Set<String> supportedMinecraftVersions,
            List<Dependency> dependencies,
            List<String> warnings) {
        private Descriptor {
            supportedLoaders = Set.copyOf(supportedLoaders);
            supportedServerTypes = Set.copyOf(supportedServerTypes);
            supportedMinecraftVersions = Set.copyOf(supportedMinecraftVersions);
            dependencies = List.copyOf(dependencies);
            warnings = List.copyOf(warnings);
        }

        private void validate(String minecraftVersion, String serverType, String modLoader) {
            if (!supportedMinecraftVersions.contains("*")
                    && supportedMinecraftVersions.stream().noneMatch(version -> matchesVersion(minecraftVersion, version))) {
                throw new IllegalArgumentException(source + ":" + id + " does not support Minecraft " + minecraftVersion);
            }
            if (kind.equals("mod") && !supportedLoaders.contains(modLoader)) {
                throw new IllegalArgumentException(source + ":" + id + " does not support mod loader " + modLoader);
            }
            if (kind.equals("plugin") && !supportedServerTypes.contains(serverType)) {
                throw new IllegalArgumentException(source + ":" + id + " does not support server type " + serverType);
            }
        }

        private static boolean matchesVersion(String actual, String supported) {
            return actual.equals(supported) || actual.startsWith(supported + ".");
        }
    }

    private record Dependency(String kind, String source, String id) {
    }
}

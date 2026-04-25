package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MinecraftMarketplaceProfile(
        String schemaVersion,
        String mode,
        List<String> sources,
        Map<String, String> searchEndpoints,
        String dependencyStrategy,
        String installPolicy,
        String malwareScan,
        String rollbackPolicy,
        List<String> installPlan,
        List<String> databaseTables,
        String evidence) {
    public MinecraftMarketplaceProfile {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        mode = requireText(mode, "mode");
        sources = List.copyOf(Objects.requireNonNull(sources, "sources must not be null"));
        searchEndpoints = copyMap(searchEndpoints, "searchEndpoints");
        dependencyStrategy = requireText(dependencyStrategy, "dependencyStrategy");
        installPolicy = requireText(installPolicy, "installPolicy");
        malwareScan = requireText(malwareScan, "malwareScan");
        rollbackPolicy = requireText(rollbackPolicy, "rollbackPolicy");
        installPlan = List.copyOf(Objects.requireNonNull(installPlan, "installPlan must not be null"));
        databaseTables = List.copyOf(Objects.requireNonNull(databaseTables, "databaseTables must not be null"));
        evidence = requireText(evidence, "evidence");
    }

    public static MinecraftMarketplaceProfile fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String mode = requirePlanned(planned, "marketplaceMode");
        List<String> sources = splitList(requirePlanned(planned, "marketplaceSources"));
        Map<String, String> endpoints = searchEndpoints(mode, sources);
        return new MinecraftMarketplaceProfile(
                "daisyminecraft.marketplace.v1",
                mode,
                sources,
                endpoints,
                "resolve-transitive-compatible-with-content-lock",
                requirePlanned(planned, "marketplaceInstallPolicy"),
                requirePlanned(planned, "marketplaceMalwareScan"),
                requirePlanned(planned, "marketplaceRollbackPolicy"),
                installSteps(),
                List.of("marketplace_items", "content_installs", "rollback_points"),
                "mode=" + mode
                        + ";sources=" + sources.size()
                        + ";dependencyResolution=transitive"
                        + ";rollback=snapshot");
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("marketplaceSchema", schemaVersion);
        attributes.put("marketplaceMode", mode);
        attributes.put("marketplaceSources", String.join(",", sources));
        attributes.put("marketplaceSearchEndpoints", joinMap(searchEndpoints));
        attributes.put("marketplaceDependencyStrategy", dependencyStrategy);
        attributes.put("marketplaceInstallPolicy", installPolicy);
        attributes.put("marketplaceMalwareScan", malwareScan);
        attributes.put("marketplaceRollbackPolicy", rollbackPolicy);
        attributes.put("marketplaceInstallPlan", String.join(",", installPlan));
        attributes.put("marketplaceDatabaseTables", String.join(",", databaseTables));
        attributes.put("marketplaceEvidence", evidence);
        return Map.copyOf(attributes);
    }

    private static Map<String, String> searchEndpoints(String mode, List<String> sources) {
        Map<String, String> endpoints = new LinkedHashMap<>();
        if ("offline".equals(mode)) {
            endpoints.put("offline", "embedded-content-catalog");
            return endpoints;
        }
        for (String source : sources) {
            endpoints.put(source, switch (source) {
                case "modrinth" -> "https://api.modrinth.com/v2/search";
                case "curseforge" -> "https://api.curseforge.com/v1/mods/search";
                case "spigotmc" -> "https://api.spiget.org/v2/search/resources";
                case "ftb" -> "ftb-app://modpacks/search";
                case "technic" -> "technic://modpacks/search";
                case "atlauncher" -> "atlauncher://packs/search";
                case "custom" -> "daisycloud://minecraft/custom-content";
                default -> throw new IllegalArgumentException("unsupported marketplace source: " + source);
            });
        }
        if ("hybrid".equals(mode)) {
            endpoints.put("offline", "embedded-content-catalog");
        }
        return endpoints;
    }

    private static List<String> installSteps() {
        return List.of(
                "search",
                "select",
                "preview-dependencies",
                "backup",
                "download",
                "malware-scan",
                "write-content-lock",
                "install",
                "restart",
                "health-check",
                "rollback-on-failure");
    }

    private static List<String> splitList(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static Map<String, String> copyMap(Map<String, String> values, String name) {
        Map<String, String> copied = new LinkedHashMap<>(Objects.requireNonNull(values, name + " must not be null"));
        for (Map.Entry<String, String> entry : copied.entrySet()) {
            requireText(entry.getKey(), name + " key");
            Objects.requireNonNull(entry.getValue(), name + " value must not be null");
        }
        return Map.copyOf(copied);
    }

    private static String joinMap(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static String requirePlanned(Map<String, String> planned, String name) {
        return requireText(planned.get(name), name);
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

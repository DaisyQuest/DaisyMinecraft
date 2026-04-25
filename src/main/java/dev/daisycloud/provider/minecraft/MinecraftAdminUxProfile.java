package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MinecraftAdminUxProfile(
        String schemaVersion,
        String state,
        List<String> realtimeChannels,
        List<String> fileTools,
        List<String> configTools,
        List<String> playerTools,
        List<String> worldTools,
        List<String> databaseTables,
        String evidence) {
    public MinecraftAdminUxProfile {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        state = requireText(state, "state");
        realtimeChannels = List.copyOf(Objects.requireNonNull(realtimeChannels, "realtimeChannels must not be null"));
        fileTools = List.copyOf(Objects.requireNonNull(fileTools, "fileTools must not be null"));
        configTools = List.copyOf(Objects.requireNonNull(configTools, "configTools must not be null"));
        playerTools = List.copyOf(Objects.requireNonNull(playerTools, "playerTools must not be null"));
        worldTools = List.copyOf(Objects.requireNonNull(worldTools, "worldTools must not be null"));
        databaseTables = List.copyOf(Objects.requireNonNull(databaseTables, "databaseTables must not be null"));
        evidence = requireText(evidence, "evidence");
    }

    public static MinecraftAdminUxProfile fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String adminPanel = requirePlanned(planned, "adminPanel");
        if ("disabled".equals(adminPanel)) {
            return new MinecraftAdminUxProfile(
                    "daisyminecraft.admin-ux.v1",
                    "disabled",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    "adminPanel=disabled;interactiveWorkflows=not-planned");
        }

        List<String> realtimeChannels = List.of("console", "logs", "health", "players", "install-progress");
        List<String> fileTools = List.of("editor", "search", "upload", "download", "permissions", "archive");
        List<String> configTools = List.of("server-properties", "yaml", "json", "toml", "plugin-config", "restart-prompts");
        List<String> playerTools = List.of("whitelist", "ops", "bans", "kick", "message", "geyser-link");
        List<String> worldTools = List.of("backups", "restore-preview", "world-import", "instance-switch", "crash-scan");
        return new MinecraftAdminUxProfile(
                "daisyminecraft.admin-ux.v1",
                "enabled",
                realtimeChannels,
                fileTools,
                configTools,
                playerTools,
                worldTools,
                List.of("admin_audit_events", "console_events", "player_actions", "runtime_health"),
                "realtime=" + realtimeChannels.size()
                        + ";fileTools=" + fileTools.size()
                        + ";playerTools=" + playerTools.size()
                        + ";worldTools=" + worldTools.size()
                        + ";browserWorkflows=planned");
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("adminPanelUxSchema", schemaVersion);
        attributes.put("adminPanelUxState", state);
        attributes.put("adminPanelRealtimeChannels", String.join(",", realtimeChannels));
        attributes.put("adminPanelFileTools", String.join(",", fileTools));
        attributes.put("adminPanelConfigTools", String.join(",", configTools));
        attributes.put("adminPanelPlayerTools", String.join(",", playerTools));
        attributes.put("adminPanelWorldTools", String.join(",", worldTools));
        attributes.put("adminPanelDatabaseTables", String.join(",", databaseTables));
        attributes.put("adminPanelWorkflowEvidence", evidence);
        return Map.copyOf(attributes);
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

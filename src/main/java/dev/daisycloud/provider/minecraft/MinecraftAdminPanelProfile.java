package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MinecraftAdminPanelProfile(
        String state,
        String url,
        String accessMode,
        String consoleAccess,
        String fileAccess,
        String subUserAccess,
        boolean twoFactorRequired,
        Map<String, String> routes,
        Map<String, String> permissionTiers,
        List<String> auditEvents,
        List<String> supportBundleFiles) {
    public MinecraftAdminPanelProfile {
        state = requireText(state, "state");
        url = Objects.requireNonNull(url, "url must not be null").trim();
        accessMode = requireText(accessMode, "accessMode");
        consoleAccess = requireText(consoleAccess, "consoleAccess");
        fileAccess = requireText(fileAccess, "fileAccess");
        subUserAccess = requireText(subUserAccess, "subUserAccess");
        routes = copyMap(routes, "routes");
        permissionTiers = copyMap(permissionTiers, "permissionTiers");
        auditEvents = List.copyOf(Objects.requireNonNull(auditEvents, "auditEvents must not be null"));
        supportBundleFiles = List.copyOf(Objects.requireNonNull(supportBundleFiles, "supportBundleFiles must not be null"));
    }

    public static MinecraftAdminPanelProfile fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String adminPanel = requirePlanned(planned, "adminPanel");
        String panelAccess = requirePlanned(planned, "panelAccess");
        String consoleAccess = requirePlanned(planned, "consoleAccess");
        String fileAccess = requirePlanned(planned, "fileAccess");
        String subUserAccess = requirePlanned(planned, "subUserAccess");
        boolean twoFactorRequired = Boolean.parseBoolean(requirePlanned(planned, "twoFactorRequired"));
        String panelUrl = planned.getOrDefault("panelUrl", "");
        if ("disabled".equals(adminPanel)) {
            return new MinecraftAdminPanelProfile(
                    "disabled",
                    "",
                    panelAccess,
                    consoleAccess,
                    fileAccess,
                    subUserAccess,
                    twoFactorRequired,
                    Map.of(),
                    Map.of(),
                    List.of(),
                    List.of());
        }

        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("overview", "/overview");
        routes.put("metrics", "/metrics");
        routes.put("logs", "/logs");
        if (!"disabled".equals(consoleAccess)) {
            routes.put("console", "/console");
        }
        if (!"disabled".equals(fileAccess)) {
            routes.put("files", "/files");
            routes.put("config", "/config/server.properties");
        }
        routes.put("players", "/players");
        routes.put("backups", "/backups");
        routes.put("content", "/content/mods-and-plugins");
        if ("enabled".equals(subUserAccess)) {
            routes.put("users", "/users");
        }
        routes.put("support", "/support/bundle");

        Map<String, String> permissionTiers = new LinkedHashMap<>();
        permissionTiers.put("owner", ownerPermissions(consoleAccess, fileAccess, subUserAccess));
        permissionTiers.put("operator", operatorPermissions(consoleAccess, fileAccess));
        permissionTiers.put("viewer", "overview:read,metrics:read,logs:read,players:read,backups:read,content:read");

        return new MinecraftAdminPanelProfile(
                "enabled",
                panelUrl,
                panelAccess,
                consoleAccess,
                fileAccess,
                subUserAccess,
                twoFactorRequired,
                routes,
                permissionTiers,
                List.of(
                        "panel.login",
                        "console.command",
                        "file.write",
                        "config.update",
                        "player.action",
                        "backup.restore",
                        "content.change",
                        "user.permission.change"),
                List.of(
                        "logs/latest.log",
                        "crash-reports/",
                        "server.properties",
                        "content-lock.json",
                        "daisycloud-runtime.properties",
                        "node-agent-task.properties"));
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("adminPanelState", state);
        attributes.put("adminPanelUrl", url);
        attributes.put("adminPanelAccessMode", accessMode);
        attributes.put("adminPanelRoutes", joinMap(routes));
        attributes.put("adminPanelPermissionTiers", joinMap(permissionTiers));
        attributes.put("adminPanelAuditEvents", String.join(",", auditEvents));
        attributes.put("adminPanelSecurityPolicy", securityPolicy());
        attributes.put("adminPanelSupportBundle", String.join(",", supportBundleFiles));
        attributes.put("adminPanelFeatureCount", Integer.toString(routes.size()));
        return Map.copyOf(attributes);
    }

    private String securityPolicy() {
        return "access=" + accessMode
                + ";twoFactor=" + (twoFactorRequired ? "required" : "optional")
                + ";sessionAudit=enabled"
                + ";commandAudit=" + ("disabled".equals(consoleAccess) ? "disabled" : "enabled")
                + ";fileAudit=" + ("disabled".equals(fileAccess) ? "disabled" : "enabled");
    }

    private static String ownerPermissions(String consoleAccess, String fileAccess, String subUserAccess) {
        StringBuilder permissions = new StringBuilder("overview:read,metrics:read,logs:read,players:write,backups:write,content:write");
        if (!"disabled".equals(consoleAccess)) {
            permissions.append(",console:").append(consoleAccess);
        }
        if (!"disabled".equals(fileAccess)) {
            permissions.append(",files:write,config:write");
        }
        if ("enabled".equals(subUserAccess)) {
            permissions.append(",users:write");
        }
        return permissions.toString();
    }

    private static String operatorPermissions(String consoleAccess, String fileAccess) {
        StringBuilder permissions = new StringBuilder("overview:read,metrics:read,logs:read,players:write,backups:read,content:read");
        if ("write".equals(consoleAccess)) {
            permissions.append(",console:write");
        } else if ("read".equals(consoleAccess)) {
            permissions.append(",console:read");
        }
        if (!"disabled".equals(fileAccess)) {
            permissions.append(",files:read,config:read");
        }
        return permissions.toString();
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

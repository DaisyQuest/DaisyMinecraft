package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MinecraftStartupFiles(Map<String, String> files) {
    public MinecraftStartupFiles {
        Map<String, String> copied = new LinkedHashMap<>(Objects.requireNonNull(files, "files must not be null"));
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("files must not be empty");
        }
        for (Map.Entry<String, String> entry : copied.entrySet()) {
            requireText(entry.getKey(), "file name");
            Objects.requireNonNull(entry.getValue(), "file content must not be null");
        }
        files = Map.copyOf(copied);
    }

    public static MinecraftStartupFiles fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        Map<String, String> files = new LinkedHashMap<>();
        files.put("eula.txt", "eula=true\n");
        files.put("server.properties", serverProperties(planned));
        files.put("content-lock.json", contentLock(planned));
        files.put("daisycloud-runtime.properties", runtimeProperties(planned));
        files.putAll(MinecraftBundledAddons.startupFiles(planned));
        return new MinecraftStartupFiles(files);
    }

    private static String serverProperties(Map<String, String> planned) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("server-name", value(planned, "serverName"));
        properties.put("motd", value(planned, "motd"));
        properties.put("server-port", value(planned, "port"));
        properties.put("max-players", value(planned, "maxPlayers"));
        properties.put("gamemode", value(planned, "gamemode"));
        properties.put("difficulty", value(planned, "difficulty"));
        properties.put("online-mode", value(planned, "onlineMode"));
        properties.put("white-list", value(planned, "enableWhitelist"));
        properties.put("pvp", value(planned, "pvp"));
        properties.put("enable-command-block", value(planned, "enableCommandBlock"));
        properties.put("view-distance", value(planned, "viewDistance"));
        properties.put("simulation-distance", value(planned, "simulationDistance"));
        return propertiesFile(properties);
    }

    private static String runtimeProperties(Map<String, String> planned) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("provider", MinecraftServerProviderCatalog.PROVIDER_ID);
        properties.put("resourceType", MinecraftServerProviderCatalog.SERVER_RESOURCE_TYPE);
        properties.put("resourceId", planned.getOrDefault("resourceId", ""));
        properties.put("containerRuntime", value(planned, "containerRuntime"));
        properties.put("serverImage", value(planned, "serverImage"));
        properties.put("backupPolicy", value(planned, "backupPolicy"));
        properties.put("backupPolicySchema", value(planned, "backupPolicySchema"));
        properties.put("backupScope", value(planned, "backupScope"));
        properties.put("backupStorage", value(planned, "backupStorage"));
        properties.put("backupCompression", value(planned, "backupCompression"));
        properties.put("backupBeforeDestructiveChanges", value(planned, "backupBeforeDestructiveChanges"));
        properties.put("backupRecoveryPointObjective", value(planned, "backupRecoveryPointObjective"));
        properties.put("networkMode", value(planned, "networkMode"));
        properties.put("networkPolicySchema", value(planned, "networkPolicySchema"));
        properties.put("networkExposure", value(planned, "networkExposure"));
        properties.put("networkFirewallRules", value(planned, "networkFirewallRules"));
        properties.put("networkDaisyNetworkBinding", value(planned, "networkDaisyNetworkBinding"));
        properties.put("networkTlsMode", value(planned, "networkTlsMode"));
        properties.put("adminPanel", value(planned, "adminPanel"));
        properties.put("panelAccess", value(planned, "panelAccess"));
        properties.put("adminPanelState", value(planned, "adminPanelState"));
        properties.put("adminPanelRoutes", value(planned, "adminPanelRoutes"));
        properties.put("adminPanelPermissionTiers", value(planned, "adminPanelPermissionTiers"));
        properties.put("adminPanelSecurityPolicy", value(planned, "adminPanelSecurityPolicy"));
        properties.put("adminPanelUxSchema", value(planned, "adminPanelUxSchema"));
        properties.put("adminPanelRealtimeChannels", value(planned, "adminPanelRealtimeChannels"));
        properties.put("adminPanelFileTools", value(planned, "adminPanelFileTools"));
        properties.put("adminPanelPlayerTools", value(planned, "adminPanelPlayerTools"));
        properties.put("adminPanelWorldTools", value(planned, "adminPanelWorldTools"));
        properties.put("databaseMode", value(planned, "databaseMode"));
        properties.put("databaseProvider", value(planned, "databaseProvider"));
        properties.put("databaseResourceId", value(planned, "databaseResourceId"));
        properties.put("databaseEndpoint", value(planned, "databaseEndpoint"));
        properties.put("databaseSchemaVersion", value(planned, "databaseSchemaVersion"));
        properties.put("databaseTables", value(planned, "databaseTables"));
        properties.put("marketplaceSchema", value(planned, "marketplaceSchema"));
        properties.put("marketplaceMode", value(planned, "marketplaceMode"));
        properties.put("marketplaceSources", value(planned, "marketplaceSources"));
        properties.put("marketplaceInstallPlan", value(planned, "marketplaceInstallPlan"));
        properties.put("marketplaceDependencyStrategy", value(planned, "marketplaceDependencyStrategy"));
        properties.put("marketplaceMalwareScan", value(planned, "marketplaceMalwareScan"));
        properties.put("marketplaceRollbackPolicy", value(planned, "marketplaceRollbackPolicy"));
        properties.put("instanceManagerSchema", value(planned, "instanceManagerSchema"));
        properties.put("instanceManagerState", value(planned, "instanceManagerState"));
        properties.put("activeInstance", value(planned, "activeInstance"));
        properties.put("maxInstances", value(planned, "maxInstances"));
        properties.put("instanceSwitchPolicy", value(planned, "instanceSwitchPolicy"));
        properties.put("bundledAddonSchema", value(planned, "bundledAddonSchema"));
        properties.put("bundledAddonIds", value(planned, "bundledAddonIds"));
        properties.put("bundledPlugins", value(planned, "bundledPlugins"));
        properties.put("bundledAddonPlan", value(planned, "bundledAddonPlan"));
        properties.put("daisyCompanion", value(planned, "daisyCompanion"));
        properties.put("daisyCompanionPluginPath", value(planned, "daisyCompanionPluginPath"));
        properties.put("daisyCompanionConfigPath", value(planned, "daisyCompanionConfigPath"));
        properties.put("daisyCompanionSha256", value(planned, "daisyCompanionSha256"));
        properties.put("daisyCompanionDogName", value(planned, "daisyCompanionDogName"));
        properties.put("daisyCompanionHealthMultiplier", value(planned, "daisyCompanionHealthMultiplier"));
        properties.put("daisyCompanionScaleMultiplier", value(planned, "daisyCompanionScaleMultiplier"));
        return propertiesFile(properties);
    }

    private static String propertiesFile(Map<String, String> properties) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            builder.append(property.getKey())
                    .append('=')
                    .append(escapeProperty(property.getValue()))
                    .append('\n');
        }
        return builder.toString();
    }

    private static String contentLock(Map<String, String> planned) {
        return MinecraftContentLock.fromPlannedAttributes(planned).toJson();
    }

    private static String value(Map<String, String> planned, String name) {
        String value = planned.get(name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required for startup file rendering");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static String escapeProperty(String value) {
        return value.replace("\\", "\\\\")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}

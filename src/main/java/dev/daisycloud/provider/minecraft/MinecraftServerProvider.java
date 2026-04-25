package dev.daisycloud.provider.minecraft;

import dev.daisycloud.provider.spi.ProviderErrorClassification;
import dev.daisycloud.provider.spi.ProviderRegistration;
import dev.daisycloud.provider.spi.ProviderRequestContext;
import dev.daisycloud.provider.spi.ProviderResponse;
import dev.daisycloud.provider.spi.ResourceProvider;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MinecraftServerProvider implements ResourceProvider {
    private static final Pattern DNS_TOKEN = Pattern.compile("[a-z][a-z0-9-]{2,62}");
    private static final Pattern RESOURCE_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");
    private static final Pattern VERSION = Pattern.compile("(latest|stable|\\d+\\.\\d+(\\.\\d+)?)");
    private static final Set<String> EDITIONS = Set.of("java", "bedrock");
    private static final Set<String> SERVER_TYPES = Set.of(
            "vanilla", "paper", "spigot", "purpur", "forge", "fabric", "quilt", "neoforge", "bedrock", "pocketmine");
    private static final Set<String> MOD_SERVER_TYPES = Set.of("forge", "fabric", "quilt", "neoforge");
    private static final Set<String> PLUGIN_SERVER_TYPES = Set.of("paper", "spigot", "purpur");
    private static final Set<String> MOD_SOURCES = Set.of("none", "modrinth", "curseforge", "custom", "ftb", "technic", "atlauncher");
    private static final Set<String> ACCESS_MODES = Set.of("public", "internal", "disabled");
    private static final Set<String> ENABLED_MODES = Set.of("enabled", "disabled");
    private static final Set<String> CONSOLE_ACCESS = Set.of("write", "read", "disabled");
    private static final Set<String> FILE_ACCESS = Set.of("web", "sftp", "both", "disabled");
    private static final Set<String> BACKUP_SCHEDULES = Set.of("none", "hourly", "daily", "weekly");
    private static final Set<String> BACKUP_STORAGE = Set.of("local", "offsite", "replicated");
    private static final Set<String> BACKUP_COMPRESSION = Set.of("none", "zstd", "gzip");
    private static final Set<String> DDOS_POLICIES = Set.of("none", "basic", "advanced");
    private static final Set<String> NETWORK_MODES = Set.of("public", "private", "disabled");
    private static final Set<String> IMPORT_MODES = Set.of("world-only", "world-and-config", "full-server");
    private static final Set<String> EXPORT_FORMATS = Set.of("daisyminecraft-state-v1", "portable-zip");
    private static final Set<String> DATABASE_MODES = Set.of("managed", "external", "disabled");
    private static final Set<String> MARKETPLACE_MODES = Set.of("offline", "live", "hybrid");
    private static final Set<String> MARKETPLACE_SOURCES = Set.of(
            "modrinth", "curseforge", "spigotmc", "ftb", "technic", "atlauncher", "custom");
    private static final Set<String> MARKETPLACE_INSTALL_POLICIES = Set.of(
            "one-click-with-preview", "manual-approval", "disabled");
    private static final Set<String> MARKETPLACE_SCAN_POLICIES = Set.of("required", "advisory", "disabled");
    private static final Set<String> MARKETPLACE_ROLLBACK_POLICIES = Set.of(
            "snapshot-before-install", "manual-snapshot", "disabled");
    private static final Set<String> INSTANCE_SWITCH_POLICIES = Set.of(
            "backup-stop-switch-start-healthcheck", "stop-switch-start", "manual");

    @Override
    public ProviderRegistration registration() {
        return MinecraftServerProviderCatalog.registration();
    }

    @Override
    public ProviderResponse<Map<String, String>> validate(ProviderRequestContext context) {
        try {
            MinecraftServerPlan.from(context.attributes()).toAttributes(context.resourceId());
            return ProviderResponse.success(context.attributes());
        } catch (IllegalArgumentException error) {
            return validationFailure(error.getMessage());
        }
    }

    @Override
    public ProviderResponse<Map<String, String>> plan(ProviderRequestContext context) {
        try {
            return ProviderResponse.success(MinecraftServerPlan.from(context.attributes()).toAttributes(context.resourceId()));
        } catch (IllegalArgumentException error) {
            return validationFailure(error.getMessage());
        }
    }

    @Override
    public ProviderResponse<Map<String, String>> apply(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        MinecraftContainerManifest manifest = MinecraftContainerManifest.fromPlannedAttributes(plan.value());
        MinecraftNodeAgentTask task = MinecraftNodeAgentTask.createOrReconcile(context.requestId(), plan.value(), manifest);
        Map<String, String> applied = new LinkedHashMap<>(plan.value());
        applied.putAll(manifest.toProviderAttributes(context.requestId()));
        applied.putAll(task.toProviderAttributes());
        applied.put("provisioningState", "Accepted");
        applied.put("observedState", "NodeAgentPending");
        return ProviderResponse.success(Map.copyOf(applied));
    }

    @Override
    public ProviderResponse<Map<String, String>> observe(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        MinecraftContainerManifest manifest = MinecraftContainerManifest.fromPlannedAttributes(plan.value());
        MinecraftNodeAgentTask task = MinecraftNodeAgentTask.createOrReconcile(context.requestId(), plan.value(), manifest);
        Map<String, String> observed = new LinkedHashMap<>(plan.value());
        observed.putAll(manifest.toProviderAttributes(context.requestId()));
        observed.putAll(task.toProviderAttributes());
        observed.put("provisioningState", "Accepted");
        observed.put("observedState", "NodeAgentPending");
        observed.put("processState", "not-started");
        observed.put("healthState", "unknown-until-node-agent-applies");
        observed.put("lastObservedRequestId", context.requestId());
        return ProviderResponse.success(Map.copyOf(observed));
    }

    @Override
    public ProviderResponse<Map<String, String>> delete(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        MinecraftContainerManifest manifest = MinecraftContainerManifest.fromPlannedAttributes(plan.value());
        MinecraftNodeAgentTask task = MinecraftNodeAgentTask.delete(context.requestId(), plan.value(), manifest);
        Map<String, String> deleted = new LinkedHashMap<>(plan.value());
        deleted.putAll(manifest.toProviderAttributes(context.requestId()));
        deleted.putAll(task.toProviderAttributes());
        deleted.put("provisioningState", "Deleting");
        deleted.put("observedState", "NodeAgentPendingDelete");
        deleted.put("deleteMode", "preserve-world-volume");
        deleted.put("deleteRequiresBackupCheck", "true");
        return ProviderResponse.success(Map.copyOf(deleted));
    }

    @Override
    public ProviderResponse<Map<String, String>> backup(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        Map<String, String> backup = new LinkedHashMap<>(plan.value());
        backup.put("backupRequestId", context.requestId());
        backup.put("backupRestorable", "true");
        return ProviderResponse.success(Map.copyOf(backup));
    }

    @Override
    public ProviderResponse<Map<String, String>> importState(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        try {
            String importMode = enumValue(valueOrDefault(context.attributes(), "importMode", "world-and-config"),
                    IMPORT_MODES,
                    "importMode");
            String importSource = safeText(valueOrDefault(context.attributes(), "importSource", "manual-upload"),
                    "importSource",
                    240);
            Map<String, String> imported = new LinkedHashMap<>(plan.value());
            imported.put("importMode", importMode);
            imported.put("importSource", importSource);
            imported.put("importScope", importScope(importMode));
            imported.put("importRequestId", context.requestId());
            imported.put("importRequiresBackupBeforeApply", "true");
            imported.put("importContentLockAction", "0".equals(plan.value().get("contentLockItemCount"))
                    ? "no-external-content"
                    : "verify-selected-content");
            imported.put("provisioningState", "ImportPending");
            imported.put("nodeAgentAction", "import-state-preserve-backup");
            return ProviderResponse.success(Map.copyOf(imported));
        } catch (IllegalArgumentException error) {
            return validationFailure(error.getMessage());
        }
    }

    @Override
    public ProviderResponse<Map<String, String>> exportState(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        try {
            String exportFormat = enumValue(valueOrDefault(context.attributes(), "exportFormat", "daisyminecraft-state-v1"),
                    EXPORT_FORMATS,
                    "exportFormat");
            Map<String, String> exported = new LinkedHashMap<>(plan.value());
            exported.put("exportFormat", exportFormat);
            exported.put("exportScope", "world,config,mods,plugins,server-properties,content-lock,admin-panel-profile,daisybase-control-plane,instances");
            exported.put("exportRequestId", context.requestId());
            exported.put("exportSchema", "daisyminecraft.export.v1");
            exported.put("exportRestorable", "true");
            exported.put("exportContentLockDigest", plan.value().get("contentLockDigest"));
            exported.put("exportIncludesAdminPanelProfile", "true");
            return ProviderResponse.success(Map.copyOf(exported));
        } catch (IllegalArgumentException error) {
            return validationFailure(error.getMessage());
        }
    }

    @Override
    public ProviderResponse<Map<String, String>> restore(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        Map<String, String> restore = new LinkedHashMap<>(plan.value());
        restore.put("restoreMode", valueOrDefault(context.attributes(), "restoreMode", "replace-world-keep-panel-policy"));
        restore.put("restoreRequestId", context.requestId());
        restore.put("restoreRequiresStop", "true");
        return ProviderResponse.success(Map.copyOf(restore));
    }

    @Override
    public ProviderResponse<Map<String, String>> diagnose(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> plan = plan(context);
        if (!plan.success()) {
            return plan;
        }
        Map<String, String> diagnostics = new LinkedHashMap<>(plan.value());
        diagnostics.put("healthSignals", "process,console,tps,mspt,players,heap,disk,backup-age,mod-resolution,network,daisybase,marketplace,instance");
        diagnostics.put("supportBundle", "logs,crash-reports,server-properties,mod-list,plugin-list,latest-backup,resource-plan,daisybase-schema,admin-audit-events");
        diagnostics.put("diagnosticRunbook", "minecraft-server-triage");
        return ProviderResponse.success(Map.copyOf(diagnostics));
    }

    private static ProviderResponse<Map<String, String>> validationFailure(String message) {
        return ProviderResponse.failure(ProviderErrorClassification.VALIDATION, message);
    }

    private record MinecraftServerPlan(
            String serverName,
            String edition,
            String minecraftVersion,
            String serverType,
            String modSource,
            String modpackId,
            List<String> selectedMods,
            List<String> selectedPlugins,
            String daisyCompanion,
            int memoryMb,
            int vcpu,
            int storageGb,
            int maxPlayers,
            int port,
            String region,
            String adminPanel,
            String panelAccess,
            String consoleAccess,
            String fileAccess,
            String subUserAccess,
            boolean twoFactorRequired,
            String backupSchedule,
            int backupRetentionDays,
            String backupStorage,
            String backupCompression,
            boolean backupBeforeDestructiveChanges,
            String ddosProtection,
            String networkMode,
            boolean enableWhitelist,
            boolean onlineMode,
            boolean pvp,
            boolean enableCommandBlock,
            String gamemode,
            String difficulty,
            int viewDistance,
            int simulationDistance,
            String motd,
            String javaVersion,
            String jvmArgs,
            String restartSchedule,
            String databaseMode,
            String databaseResourceId,
            String databaseName,
            String databaseEndpoint,
            String databaseSku,
            String marketplaceMode,
            List<String> marketplaceSources,
            String marketplaceInstallPolicy,
            String marketplaceMalwareScan,
            String marketplaceRollbackPolicy,
            String instanceManager,
            String activeInstance,
            int maxInstances,
            String instanceSwitchPolicy) {
        private static MinecraftServerPlan from(Map<String, String> attributes) {
            Objects.requireNonNull(attributes, "attributes must not be null");
            String serverName = required(attributes, "serverName");
            if (!DNS_TOKEN.matcher(serverName).matches()) {
                throw new IllegalArgumentException("serverName must be a DNS-safe lowercase token with 3-63 characters");
            }
            if (!parseBoolean(required(attributes, "eulaAccepted"), "eulaAccepted")) {
                throw new IllegalArgumentException("eulaAccepted must be true before provisioning a Minecraft server");
            }

            String edition = enumValue(optional(attributes, "edition", "java"), EDITIONS, "edition");
            String minecraftVersion = required(attributes, "minecraftVersion");
            if (!VERSION.matcher(minecraftVersion).matches()) {
                throw new IllegalArgumentException("minecraftVersion must be latest, stable, or a numeric Minecraft version");
            }
            String serverType = enumValue(optional(attributes, "serverType", edition.equals("bedrock") ? "bedrock" : "paper"),
                    SERVER_TYPES,
                    "serverType");
            validateEditionCompatibility(edition, serverType);

            List<String> selectedMods = tokenList(attributes.get("selectedMods"), "selectedMods");
            List<String> selectedPlugins = tokenList(attributes.get("selectedPlugins"), "selectedPlugins");
            String modpackId = optional(attributes, "modpackId", "");
            if (!modpackId.isBlank() && !RESOURCE_TOKEN.matcher(modpackId).matches()) {
                throw new IllegalArgumentException("modpackId must be a safe content identifier");
            }
            String modSource = enumValue(optional(attributes, "modSource", defaultModSource(modpackId, selectedMods)),
                    MOD_SOURCES,
                    "modSource");
            validateContentSelection(serverType, modSource, modpackId, selectedMods, selectedPlugins);
            String daisyCompanion = enumValue(optional(attributes,
                            "daisyCompanion",
                            MinecraftBundledAddons.defaultDaisyCompanionMode(serverType)),
                    ENABLED_MODES,
                    "daisyCompanion");
            MinecraftBundledAddons.validateDaisyCompanion(daisyCompanion, serverType);

            int memoryMb = boundedInt(optional(attributes, "memoryMb", defaultMemoryMb(modpackId, selectedMods)),
                    "memoryMb", 512, 262_144);
            int vcpu = boundedInt(optional(attributes, "vcpu", "2"), "vcpu", 1, 64);
            int storageGb = boundedInt(optional(attributes, "storageGb", "20"), "storageGb", 5, 2_048);
            int maxPlayers = boundedInt(optional(attributes, "maxPlayers", "20"), "maxPlayers", 1, 10_000);
            int port = boundedInt(optional(attributes, "port", "25565"), "port", 1, 65_535);
            String region = optional(attributes, "region", "local");
            if (!RESOURCE_TOKEN.matcher(region).matches()) {
                throw new IllegalArgumentException("region must be a safe region token");
            }

            String adminPanel = enumValue(optional(attributes, "adminPanel", "enabled"), ENABLED_MODES, "adminPanel");
            String panelAccess = enumValue(optional(attributes, "panelAccess", "internal"), ACCESS_MODES, "panelAccess");
            String consoleAccess = enumValue(optional(attributes, "consoleAccess", "write"), CONSOLE_ACCESS, "consoleAccess");
            String fileAccess = enumValue(optional(attributes, "fileAccess", "web"), FILE_ACCESS, "fileAccess");
            String subUserAccess = enumValue(optional(attributes, "subUserAccess", "enabled"), ENABLED_MODES, "subUserAccess");
            boolean twoFactorRequired = parseBoolean(optional(attributes, "twoFactorRequired", "true"), "twoFactorRequired");
            if ("enabled".equals(adminPanel) && "public".equals(panelAccess) && !twoFactorRequired) {
                throw new IllegalArgumentException("public admin panels must require twoFactorRequired=true");
            }

            String backupSchedule = enumValue(optional(attributes, "backupSchedule", "daily"), BACKUP_SCHEDULES, "backupSchedule");
            int retention = boundedInt(optional(attributes, "backupRetentionDays", "7"), "backupRetentionDays", 0, 365);
            if (!"none".equals(backupSchedule) && retention == 0) {
                throw new IllegalArgumentException("backupRetentionDays must be greater than 0 when backups are scheduled");
            }
            String backupStorage = enumValue(optional(attributes, "backupStorage", "local"), BACKUP_STORAGE, "backupStorage");
            String backupCompression = enumValue(optional(attributes, "backupCompression", "zstd"), BACKUP_COMPRESSION, "backupCompression");
            boolean backupBeforeDestructiveChanges = parseBoolean(
                    optional(attributes, "backupBeforeDestructiveChanges", "true"),
                    "backupBeforeDestructiveChanges");
            String networkMode = enumValue(optional(attributes, "networkMode", "public"), NETWORK_MODES, "networkMode");
            String ddosProtection = enumValue(optional(attributes, "ddosProtection", "advanced"), DDOS_POLICIES, "ddosProtection");
            if ("public".equals(networkMode) && "none".equals(ddosProtection)) {
                throw new IllegalArgumentException("public Minecraft endpoints must enable DDoS protection");
            }
            if ("disabled".equals(networkMode) && "public".equals(panelAccess)) {
                throw new IllegalArgumentException("disabled networkMode cannot expose a public admin panel");
            }

            boolean whitelist = parseBoolean(optional(attributes, "enableWhitelist", "false"), "enableWhitelist");
            boolean onlineMode = parseBoolean(optional(attributes, "onlineMode", "true"), "onlineMode");
            boolean pvp = parseBoolean(optional(attributes, "pvp", "true"), "pvp");
            boolean enableCommandBlock = parseBoolean(optional(attributes, "enableCommandBlock", "false"), "enableCommandBlock");
            String gamemode = enumValue(optional(attributes, "gamemode", "survival"),
                    Set.of("survival", "creative", "adventure", "spectator"),
                    "gamemode");
            String difficulty = enumValue(optional(attributes, "difficulty", "normal"),
                    Set.of("peaceful", "easy", "normal", "hard"),
                    "difficulty");
            int viewDistance = boundedInt(optional(attributes, "viewDistance", "10"), "viewDistance", 2, 32);
            int simulationDistance = boundedInt(optional(attributes, "simulationDistance", "10"), "simulationDistance", 2, 32);
            String motd = safeText(optional(attributes, "motd", "DaisyCloud Minecraft Server"), "motd", 120);
            String javaVersion = optional(attributes, "javaVersion", defaultJavaVersion(minecraftVersion));
            if (!Set.of("17", "21").contains(javaVersion)) {
                throw new IllegalArgumentException("javaVersion must be 17 or 21");
            }
            String jvmArgs = safeText(optional(attributes, "jvmArgs", ""), "jvmArgs", 1_200);
            String restartSchedule = safeText(optional(attributes, "restartSchedule", "0 5 * * *"), "restartSchedule", 120);

            String databaseMode = enumValue(optional(attributes, "databaseMode", "managed"), DATABASE_MODES, "databaseMode");
            String databaseResourceId = optional(attributes, "databaseResourceId", "");
            String databaseName = optional(attributes, "databaseName", serverName + "-control");
            if (!DNS_TOKEN.matcher(databaseName).matches()) {
                throw new IllegalArgumentException("databaseName must be a DaisyBase-safe lowercase token with 3-63 characters");
            }
            String databaseEndpoint = optional(attributes, "databaseEndpoint", "jdbc:daisybase://minecraft/" + databaseName);
            if (!databaseEndpoint.startsWith("jdbc:daisybase://")) {
                throw new IllegalArgumentException("databaseEndpoint must use jdbc:daisybase://");
            }
            String databaseSku = safeText(optional(attributes, "databaseSku", "developer"), "databaseSku", 120);
            if (!databaseSku.isBlank() && !RESOURCE_TOKEN.matcher(databaseSku).matches()) {
                throw new IllegalArgumentException("databaseSku must be a safe DaisyBase SKU token");
            }
            if ("external".equals(databaseMode) && databaseResourceId.isBlank()) {
                throw new IllegalArgumentException("databaseResourceId is required when databaseMode=external");
            }

            String marketplaceMode = enumValue(optional(attributes, "marketplaceMode", "hybrid"),
                    MARKETPLACE_MODES,
                    "marketplaceMode");
            List<String> marketplaceSources = parseMarketplaceSources(optional(attributes,
                    "marketplaceSources",
                    defaultMarketplaceSources(marketplaceMode)));
            String marketplaceInstallPolicy = enumValue(optional(attributes,
                            "marketplaceInstallPolicy",
                            "one-click-with-preview"),
                    MARKETPLACE_INSTALL_POLICIES,
                    "marketplaceInstallPolicy");
            String marketplaceMalwareScan = enumValue(optional(attributes, "marketplaceMalwareScan", "required"),
                    MARKETPLACE_SCAN_POLICIES,
                    "marketplaceMalwareScan");
            String marketplaceRollbackPolicy = enumValue(optional(attributes,
                            "marketplaceRollbackPolicy",
                            "snapshot-before-install"),
                    MARKETPLACE_ROLLBACK_POLICIES,
                    "marketplaceRollbackPolicy");

            String instanceManager = enumValue(optional(attributes, "instanceManager", "enabled"),
                    ENABLED_MODES,
                    "instanceManager");
            String activeInstance = optional(attributes, "activeInstance", "primary");
            if (!RESOURCE_TOKEN.matcher(activeInstance).matches()) {
                throw new IllegalArgumentException("activeInstance must be a safe instance token");
            }
            int maxInstances = boundedInt(optional(attributes, "maxInstances", "5"), "maxInstances", 1, 50);
            String instanceSwitchPolicy = enumValue(optional(attributes,
                            "instanceSwitchPolicy",
                            "backup-stop-switch-start-healthcheck"),
                    INSTANCE_SWITCH_POLICIES,
                    "instanceSwitchPolicy");

            return new MinecraftServerPlan(
                    serverName,
                    edition,
                    minecraftVersion,
                    serverType,
                    modSource,
                    modpackId,
                    selectedMods,
                    selectedPlugins,
                    daisyCompanion,
                    memoryMb,
                    vcpu,
                    storageGb,
                    maxPlayers,
                    port,
                    region,
                    adminPanel,
                    panelAccess,
                    consoleAccess,
                    fileAccess,
                    subUserAccess,
                    twoFactorRequired,
                    backupSchedule,
                    retention,
                    backupStorage,
                    backupCompression,
                    backupBeforeDestructiveChanges,
                    ddosProtection,
                    networkMode,
                    whitelist,
                    onlineMode,
                    pvp,
                    enableCommandBlock,
                    gamemode,
                    difficulty,
                    viewDistance,
                    simulationDistance,
                    motd,
                    javaVersion,
                    jvmArgs,
                    restartSchedule,
                    databaseMode,
                    databaseResourceId,
                    databaseName,
                    databaseEndpoint,
                    databaseSku,
                    marketplaceMode,
                    marketplaceSources,
                    marketplaceInstallPolicy,
                    marketplaceMalwareScan,
                    marketplaceRollbackPolicy,
                    instanceManager,
                    activeInstance,
                    maxInstances,
                    instanceSwitchPolicy);
        }

        private Map<String, String> toAttributes(String resourceId) {
            Map<String, String> planned = new LinkedHashMap<>();
            if (resourceId != null) {
                planned.put("resourceId", resourceId);
            }
            planned.put("serverName", serverName);
            planned.put("eulaAccepted", "true");
            planned.put("edition", edition);
            planned.put("minecraftVersion", minecraftVersion);
            planned.put("serverType", serverType);
            planned.put("modLoader", modLoader(serverType));
            planned.put("modSource", modSource);
            planned.put("modpackId", modpackId);
            planned.put("selectedMods", String.join(",", selectedMods));
            planned.put("selectedPlugins", String.join(",", selectedPlugins));
            planned.put("daisyCompanion", daisyCompanion);
            planned.put("memoryMb", Integer.toString(memoryMb));
            planned.put("vcpu", Integer.toString(vcpu));
            planned.put("storageGb", Integer.toString(storageGb));
            planned.put("maxPlayers", Integer.toString(maxPlayers));
            planned.put("port", Integer.toString(port));
            planned.put("region", region);
            planned.put("adminPanel", adminPanel);
            planned.put("panelAccess", panelAccess);
            planned.put("consoleAccess", consoleAccess);
            planned.put("fileAccess", fileAccess);
            planned.put("subUserAccess", subUserAccess);
            planned.put("twoFactorRequired", Boolean.toString(twoFactorRequired));
            planned.put("backupSchedule", backupSchedule);
            planned.put("backupRetentionDays", Integer.toString(backupRetentionDays));
            planned.put("backupPolicy", backupSchedule + "/" + backupRetentionDays + "d");
            planned.put("backupStorage", backupStorage);
            planned.put("backupCompression", backupCompression);
            planned.put("backupBeforeDestructiveChanges", Boolean.toString(backupBeforeDestructiveChanges));
            planned.putAll(MinecraftBackupPolicyProfile.fromPlannedAttributes(planned).toProviderAttributes());
            planned.put("ddosProtection", ddosProtection);
            planned.put("networkMode", networkMode);
            planned.put("enableWhitelist", Boolean.toString(enableWhitelist));
            planned.put("onlineMode", Boolean.toString(onlineMode));
            planned.put("pvp", Boolean.toString(pvp));
            planned.put("enableCommandBlock", Boolean.toString(enableCommandBlock));
            planned.put("gamemode", gamemode);
            planned.put("difficulty", difficulty);
            planned.put("viewDistance", Integer.toString(viewDistance));
            planned.put("simulationDistance", Integer.toString(simulationDistance));
            planned.put("motd", motd);
            planned.put("javaVersion", javaVersion);
            planned.put("jvmArgs", jvmArgs);
            planned.put("restartSchedule", restartSchedule);
            planned.put("containerRuntime", "DaisyCloud.OpenAppServiceContainer");
            planned.put("serverImage", "daisycloud/minecraft-" + serverType + ":" + minecraftVersion);
            planned.put("dataVolume", "daisycloud-mc-" + serverName);
            planned.put("serverEndpoint", serverEndpoint(serverName, region, port, networkMode));
            planned.put("panelUrl", "enabled".equals(adminPanel)
                    ? "https://" + serverName + ".panel." + region + ".mc.internal"
                    : "");
            planned.putAll(MinecraftBundledAddons.fromPlannedAttributes(planned));
            planned.putAll(MinecraftAdminPanelProfile.fromPlannedAttributes(planned).toProviderAttributes());
            planned.putAll(MinecraftNetworkPolicyProfile.fromPlannedAttributes(planned).toProviderAttributes());
            planned.put("databaseMode", databaseMode);
            planned.put("databaseResourceId", databaseResourceId);
            planned.put("databaseName", databaseName);
            planned.put("databaseEndpoint", databaseEndpoint);
            planned.put("databaseSku", databaseSku);
            planned.putAll(MinecraftDatabaseProfile.fromPlannedAttributes(planned, resourceId).toProviderAttributes());
            planned.put("marketplaceMode", marketplaceMode);
            planned.put("marketplaceSources", String.join(",", marketplaceSources));
            planned.put("marketplaceInstallPolicy", marketplaceInstallPolicy);
            planned.put("marketplaceMalwareScan", marketplaceMalwareScan);
            planned.put("marketplaceRollbackPolicy", marketplaceRollbackPolicy);
            planned.putAll(MinecraftMarketplaceProfile.fromPlannedAttributes(planned).toProviderAttributes());
            planned.put("instanceManager", instanceManager);
            planned.put("activeInstance", activeInstance);
            planned.put("maxInstances", Integer.toString(maxInstances));
            planned.put("instanceSwitchPolicy", instanceSwitchPolicy);
            planned.putAll(MinecraftInstanceManagerProfile.fromPlannedAttributes(planned).toProviderAttributes());
            planned.putAll(MinecraftAdminUxProfile.fromPlannedAttributes(planned).toProviderAttributes());
            MinecraftContentLock contentLock = MinecraftContentLock.fromPlannedAttributes(planned);
            planned.putAll(contentLock.toProviderAttributes());
            planned.put("resourceRecommendation", resourceRecommendation(modpackId, selectedMods));
            planned.put("compatibilityReport", compatibilityReport());
            planned.put("deploymentEvidence",
                    "EULA accepted;Runtime profile captured;Admin panel policy captured;Mod selection locked;"
                            + "Content lock digested;Backup policy captured;Network policy captured;"
                            + "DaisyBase control plane planned;Marketplace profile captured;"
                            + "Bundled companion plugin planned;"
                            + "Instance manager captured;Admin UX workflows captured;"
                            + "Container image planned;Startup files rendered");
            return Map.copyOf(planned);
        }

        private String compatibilityReport() {
            if (!modpackId.isBlank()) {
                return "modpack " + modpackId + " resolved from " + modSource + " for " + serverType;
            }
            if (!selectedMods.isEmpty()) {
                return selectedMods.size() + " mods resolved for " + modLoader(serverType);
            }
            if (!selectedPlugins.isEmpty()) {
                return selectedPlugins.size() + " plugins resolved for " + serverType;
            }
            return "no external content selected";
        }
    }

    private static void validateEditionCompatibility(String edition, String serverType) {
        if ("bedrock".equals(edition) && !Set.of("bedrock", "pocketmine").contains(serverType)) {
            throw new IllegalArgumentException("bedrock edition requires serverType bedrock or pocketmine");
        }
        if ("java".equals(edition) && Set.of("bedrock", "pocketmine").contains(serverType)) {
            throw new IllegalArgumentException("java edition cannot use Bedrock-only server types");
        }
    }

    private static void validateContentSelection(
            String serverType,
            String modSource,
            String modpackId,
            List<String> selectedMods,
            List<String> selectedPlugins) {
        if (!selectedMods.isEmpty() && !MOD_SERVER_TYPES.contains(serverType)) {
            throw new IllegalArgumentException("selectedMods require a Forge, Fabric, Quilt, or NeoForge server type");
        }
        if (!selectedPlugins.isEmpty() && !PLUGIN_SERVER_TYPES.contains(serverType)) {
            throw new IllegalArgumentException("selectedPlugins require a Paper, Spigot, or Purpur server type");
        }
        if (!modpackId.isBlank() && !selectedMods.isEmpty()) {
            throw new IllegalArgumentException("modpackId and selectedMods cannot be planned in the same change");
        }
        if (!modpackId.isBlank() && "vanilla".equals(serverType)) {
            throw new IllegalArgumentException("modpackId requires a mod-capable or plugin-capable server type");
        }
        if ((!modpackId.isBlank() || !selectedMods.isEmpty()) && "none".equals(modSource)) {
            throw new IllegalArgumentException("modSource cannot be none when mods or a modpack are selected");
        }
    }

    private static List<String> tokenList(String value, String name) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tokens = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(candidate -> !candidate.isBlank())
                .collect(Collectors.toList());
        for (String token : tokens) {
            if (!RESOURCE_TOKEN.matcher(token).matches()) {
                throw new IllegalArgumentException(name + " must contain safe content identifiers");
            }
        }
        return List.copyOf(tokens);
    }

    private static List<String> parseMarketplaceSources(String value) {
        List<String> sources = Arrays.stream(value.split(","))
                .map(candidate -> candidate.trim().toLowerCase(Locale.ROOT))
                .filter(candidate -> !candidate.isBlank())
                .collect(Collectors.toList());
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("marketplaceSources must contain at least one source");
        }
        for (String source : sources) {
            if (!MARKETPLACE_SOURCES.contains(source)) {
                throw new IllegalArgumentException("marketplaceSources must contain only supported sources: "
                        + String.join(", ", MARKETPLACE_SOURCES));
            }
        }
        return List.copyOf(sources);
    }

    private static String required(Map<String, String> attributes, String name) {
        String value = attributes.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String optional(Map<String, String> attributes, String name, String defaultValue) {
        String value = attributes.get(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String valueOrDefault(Map<String, String> attributes, String name, String defaultValue) {
        String value = attributes.get(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String enumValue(String value, Set<String> supported, String name) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!supported.contains(normalized)) {
            throw new IllegalArgumentException(name + " must be one of " + String.join(", ", supported));
        }
        return normalized;
    }

    private static boolean parseBoolean(String value, String name) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException(name + " must be true or false");
    }

    private static int boundedInt(String value, String name, int min, int max) {
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(name + " must be an integer", error);
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
        return parsed;
    }

    private static String safeText(String value, String name, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index)) && value.charAt(index) != '\t') {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return value;
    }

    private static String defaultModSource(String modpackId, List<String> selectedMods) {
        return modpackId.isBlank() && selectedMods.isEmpty() ? "none" : "modrinth";
    }

    private static String defaultMarketplaceSources(String marketplaceMode) {
        if ("offline".equals(marketplaceMode)) {
            return "custom";
        }
        return "modrinth,curseforge,spigotmc,ftb,technic,atlauncher";
    }

    private static String defaultMemoryMb(String modpackId, List<String> selectedMods) {
        if (!modpackId.isBlank()) {
            return "8192";
        }
        if (selectedMods.size() >= 20) {
            return "8192";
        }
        if (!selectedMods.isEmpty()) {
            return "4096";
        }
        return "2048";
    }

    private static String defaultJavaVersion(String minecraftVersion) {
        return minecraftVersion.startsWith("1.16") || minecraftVersion.startsWith("1.17") ? "17" : "21";
    }

    private static String modLoader(String serverType) {
        return MOD_SERVER_TYPES.contains(serverType) ? serverType : "none";
    }

    private static String resourceRecommendation(String modpackId, List<String> selectedMods) {
        if (!modpackId.isBlank()) {
            return "8GB RAM baseline; raise to 12GB+ for large expert packs or 20+ active players";
        }
        if (selectedMods.size() >= 20) {
            return "8GB RAM baseline for larger curated mod selections";
        }
        if (!selectedMods.isEmpty()) {
            return "4GB RAM baseline for light modded servers";
        }
        return "2GB RAM baseline for vanilla or plugin-light servers";
    }

    private static String serverEndpoint(String serverName, String region, int port, String networkMode) {
        return switch (networkMode) {
            case "disabled" -> "";
            case "private" -> "minecraft://" + serverName + "." + region + ".private.mc.internal:" + port;
            default -> "minecraft://" + serverName + "." + region + ".mc.internal:" + port;
        };
    }

    private static String importScope(String importMode) {
        return switch (importMode) {
            case "world-only" -> "world";
            case "world-and-config" -> "world,server-properties,whitelist,ops,bans";
            case "full-server" -> "world,server-properties,mods,plugins,content-lock,admin-panel-profile,daisybase-control-plane,instances";
            default -> throw new IllegalArgumentException("importMode must be one of " + String.join(", ", IMPORT_MODES));
        };
    }
}

package dev.daisycloud.provider.minecraft;

import dev.daisycloud.provider.daisybase.DaisyBaseConnector;
import dev.daisycloud.provider.daisybase.DaisyBaseProviderCatalog;
import dev.daisycloud.provider.daisybase.DaisyBaseSqlResult;
import dev.daisycloud.provider.spi.ProviderOperationKind;
import dev.daisycloud.provider.spi.ProviderRequestContext;
import dev.daisycloud.provider.spi.ProviderResponse;
import dev.daisycloud.state.InMemoryResourceRepository;
import dev.daisycloud.state.ResourceRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftServerProviderTest {
    @Test
    void plansContainerAdminPanelBackupsAndNetworkDefaults() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "survival-core",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "gamemode", "survival",
                "difficulty", "hard",
                "viewDistance", "12",
                "motd", "Daisy survival")));

        assertTrue(response.success(), response.message());
        assertEquals("java", response.value().get("edition"));
        assertEquals("paper", response.value().get("serverType"));
        assertEquals("DaisyCloud.OpenAppServiceContainer", response.value().get("containerRuntime"));
        assertEquals("daisycloud/minecraft-paper:1.21.11", response.value().get("serverImage"));
        assertEquals("enabled", response.value().get("adminPanel"));
        assertEquals("internal", response.value().get("panelAccess"));
        assertEquals("write", response.value().get("consoleAccess"));
        assertEquals("daily/7d", response.value().get("backupPolicy"));
        assertEquals("daisyminecraft.backup.v1", response.value().get("backupPolicySchema"));
        assertTrue(response.value().get("backupScope").contains("admin-panel-profile"));
        assertEquals("local", response.value().get("backupStorage"));
        assertEquals("zstd", response.value().get("backupCompression"));
        assertEquals("true", response.value().get("backupBeforeDestructiveChanges"));
        assertEquals("P1D", response.value().get("backupRecoveryPointObjective"));
        assertEquals("disabled", response.value().get("backupOffsitePolicy"));
        assertEquals("advanced", response.value().get("ddosProtection"));
        assertEquals("daisyminecraft.network.v1", response.value().get("networkPolicySchema"));
        assertEquals("game=public;panel=internal", response.value().get("networkExposure"));
        assertTrue(response.value().get("networkGameEndpoint").contains("survival-core.local.mc.internal:25565"));
        assertTrue(response.value().get("networkFirewallRules").contains("game-tcp=allow tcp/25565"));
        assertTrue(response.value().get("networkDaisyNetworkBinding").contains("survival-core-game"));
        assertTrue(response.value().get("networkDdosPolicy").contains("edge-scrubbing=always-on"));
        assertEquals("panel-tls-required", response.value().get("networkTlsMode"));
        assertEquals("enabled", response.value().get("adminPanelState"));
        assertTrue(response.value().get("adminPanelRoutes").contains("console=/console"));
        assertTrue(response.value().get("adminPanelRoutes").contains("files=/files"));
        assertTrue(response.value().get("adminPanelPermissionTiers").contains("owner="));
        assertTrue(response.value().get("adminPanelAuditEvents").contains("console.command"));
        assertTrue(response.value().get("adminPanelSecurityPolicy").contains("twoFactor=required"));
        assertEquals("daisyminecraft.admin-ux.v1", response.value().get("adminPanelUxSchema"));
        assertTrue(response.value().get("adminPanelRealtimeChannels").contains("install-progress"));
        assertTrue(response.value().get("adminPanelFileTools").contains("editor"));
        assertTrue(response.value().get("adminPanelPlayerTools").contains("whitelist"));
        assertTrue(response.value().get("adminPanelWorldTools").contains("instance-switch"));
        assertEquals("managed", response.value().get("databaseMode"));
        assertEquals("DaisyCloud.DaisyBase", response.value().get("databaseProvider"));
        assertEquals("databases", response.value().get("databaseResourceType"));
        assertTrue(response.value().get("databaseResourceId").contains("/providers/DaisyCloud.DaisyBase/databases/survival-core-control"));
        assertEquals("jdbc:daisybase://minecraft/survival-core-control", response.value().get("databaseEndpoint"));
        assertEquals("daisyminecraft.database.v1", response.value().get("databaseSchemaVersion"));
        assertTrue(response.value().get("databaseTables").contains("admin_audit_events"));
        assertTrue(response.value().get("databaseBootstrapSql").contains("CREATE TABLE marketplace_items"));
        assertEquals("daisyminecraft.marketplace.v1", response.value().get("marketplaceSchema"));
        assertEquals("hybrid", response.value().get("marketplaceMode"));
        assertTrue(response.value().get("marketplaceSources").contains("modrinth"));
        assertTrue(response.value().get("marketplaceSearchEndpoints").contains("curseforge=https://api.curseforge.com"));
        assertTrue(response.value().get("marketplaceInstallPlan").contains("malware-scan"));
        assertTrue(response.value().get("marketplaceDependencyStrategy").contains("transitive"));
        assertEquals("required", response.value().get("marketplaceMalwareScan"));
        assertEquals("daisyminecraft.bundled-addons.v1", response.value().get("bundledAddonSchema"));
        assertEquals("enabled", response.value().get("daisyCompanion"));
        assertEquals("1", response.value().get("bundledAddonCount"));
        assertEquals("daisyminecraft:daisy-companion", response.value().get("bundledAddonIds"));
        assertEquals("Daisy", response.value().get("daisyCompanionDogName"));
        assertEquals("100.0", response.value().get("daisyCompanionHealthMultiplier"));
        assertEquals("2.0", response.value().get("daisyCompanionScaleMultiplier"));
        assertEquals("plugins/DaisyCompanion.jar", response.value().get("daisyCompanionPluginPath"));
        assertTrue(response.value().get("daisyCompanionSha256").matches("[a-f0-9]{64}"));
        assertEquals("daisyminecraft.instances.v1", response.value().get("instanceManagerSchema"));
        assertEquals("enabled", response.value().get("instanceManagerState"));
        assertEquals("primary", response.value().get("activeInstance"));
        assertEquals("5", response.value().get("maxInstances"));
        assertTrue(response.value().get("instanceSlots").contains("rollback:reserved"));
        assertEquals("hard", response.value().get("difficulty"));
        assertEquals("12", response.value().get("viewDistance"));
        assertEquals("Daisy survival", response.value().get("motd"));
        assertTrue(response.value().get("panelUrl").contains("survival-core.panel.local.mc.internal"));
        assertTrue(response.value().get("deploymentEvidence").contains("Mod selection locked"));
        assertTrue(response.value().get("deploymentEvidence").contains("DaisyBase control plane planned"));
        assertTrue(response.value().get("deploymentEvidence").contains("Marketplace profile captured"));
        assertTrue(response.value().get("deploymentEvidence").contains("Bundled companion plugin planned"));
        assertTrue(response.value().get("deploymentEvidence").contains("Instance manager captured"));
        assertTrue(response.value().get("deploymentEvidence").contains("Startup files rendered"));
    }

    @Test
    void applyRendersNodeAgentContainerManifestAndStartupFiles() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().apply(context(Map.of(
                "serverName", "survival-core",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "maxPlayers", "42",
                "selectedPlugins", "luckperms,geyser")));

        assertTrue(response.success(), response.message());
        assertEquals("daisyminecraft.container.v1", response.value().get("containerManifestVersion"));
        assertEquals("mc-survival-core", response.value().get("containerServiceName"));
        assertEquals("daisycloud/minecraft-paper:1.21.11", response.value().get("containerImage"));
        assertTrue(response.value().get("containerCommand").contains("-jar server.jar nogui"));
        assertTrue(response.value().get("containerEnvironment").contains("DAISY_MINECRAFT_SERVER_TYPE=paper"));
        assertTrue(response.value().get("containerEnvironment").contains("DAISY_MINECRAFT_DATABASE_MODE=managed"));
        assertTrue(response.value().get("containerEnvironment").contains("DAISY_MINECRAFT_MARKETPLACE_MODE=hybrid"));
        assertTrue(response.value().get("containerEnvironment").contains("DAISY_MINECRAFT_ACTIVE_INSTANCE=primary"));
        assertTrue(response.value().get("containerEnvironment").contains("DAISY_MINECRAFT_DAISY_COMPANION=enabled"));
        assertTrue(response.value().get("containerEnvironment").contains("DAISY_MINECRAFT_BUNDLED_ADDONS=daisyminecraft:daisy-companion"));
        assertTrue(response.value().get("containerPortBindings").contains("25565/tcp=25565"));
        assertTrue(response.value().get("containerVolumes").contains("daisycloud-mc-survival-core=/data"));
        assertTrue(response.value().get("containerResourceLimits").contains("memoryMb=2048"));
        assertTrue(response.value().get("containerRestartPolicy").contains("unless-stopped"));
        assertEquals("planned-for-node-agent", response.value().get("containerApplyState"));
        assertEquals("create-or-reconcile-container", response.value().get("nodeAgentAction"));
        assertEquals("daisyminecraft.node-agent-task.v1", response.value().get("nodeAgentTaskSchema"));
        assertEquals("running", response.value().get("nodeAgentDesiredState"));
        assertTrue(response.value().get("nodeAgentTaskId").startsWith("minecraft-node-agent-mc-survival-core-"));
        assertTrue(response.value().get("nodeAgentReconcileDigest").matches("[a-f0-9]{64}"));
        assertTrue(response.value().get("nodeAgentIdempotencyKey").contains(response.value().get("nodeAgentReconcileDigest")));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("pull-image:daisycloud/minecraft-paper:1.21.11"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("prepare-daisybase:"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("resolve-marketplace-content:hybrid"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("activate-instance:primary"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("write-startup-files:"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("install-bundled-addons:install:daisyminecraft:daisy-companion"));
        assertTrue(response.value().get("nodeAgentEvidence").contains("marketplaceMode=hybrid"));
        assertTrue(response.value().get("nodeAgentEvidence").contains("adminPanelUxSchema=daisyminecraft.admin-ux.v1"));
        assertTrue(response.value().get("nodeAgentStartupFileDigests").contains("server.properties="));
        assertTrue(response.value().get("startupFileNames").contains("server.properties"));
        assertTrue(response.value().get("startupFile:server.properties").contains("max-players=42"));
        assertTrue(response.value().get("startupFile:server.properties").contains("online-mode=true"));
        assertTrue(response.value().get("startupFile:eula.txt").contains("eula=true"));
        assertTrue(response.value().get("startupFile:content-lock.json").contains("\"kind\":\"plugin\""));
        assertTrue(response.value().get("startupFile:plugins/DaisyCompanion/config.yml").contains("dog-name: Daisy"));
        assertTrue(response.value().get("startupFile:plugins/DaisyCompanion/config.yml").contains("health-multiplier: 100.0"));
        assertTrue(response.value().get("startupFile:plugins/DaisyCompanion/config.yml").contains("scale-multiplier: 2.0"));
        assertTrue(response.value().get("startupFile:daisyminecraft-bundled-addons.json").contains("\"id\": \"daisyminecraft:daisy-companion\""));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("backupPolicySchema="));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("adminPanelSecurityPolicy="));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("networkFirewallRules="));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("databaseSchemaVersion=daisyminecraft.database.v1"));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("marketplaceInstallPlan="));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("instanceManagerSchema=daisyminecraft.instances.v1"));
        assertTrue(response.value().get("startupFile:daisycloud-runtime.properties").contains("daisyCompanion=enabled"));
        assertTrue(response.value().get("startupFile:content-lock.json").contains("\"id\":\"luckperms\""));
        assertTrue(response.value().get("startupFile:content-lock.json").contains("\"id\":\"geyser\""));
    }

    @Test
    void acceptsExternalDaisyBaseControlPlaneDatabase() {
        String databaseResourceId = "/subscriptions/11111111-1111-4111-8111-111111111111/resourceGroups/rg-test/providers/"
                + "DaisyCloud.DaisyBase/databases/shared-mc";

        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "database-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "databaseMode", "external",
                "databaseResourceId", databaseResourceId,
                "databaseName", "shared-mc",
                "databaseEndpoint", "jdbc:daisybase://control/shared-mc",
                "databaseSku", "standard")));

        assertTrue(response.success(), response.message());
        assertEquals("external", response.value().get("databaseMode"));
        assertEquals(databaseResourceId, response.value().get("databaseResourceId"));
        assertEquals("jdbc:daisybase://control/shared-mc", response.value().get("databaseEndpoint"));
        assertEquals("standard", response.value().get("databaseSku"));
        assertTrue(response.value().get("databaseEvidence").contains("provider=DaisyCloud.DaisyBase"));
    }

    @Test
    void disabledDatabaseModeRemovesDaisyBaseBootstrapContract() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "database-off",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "databaseMode", "disabled")));

        assertTrue(response.success(), response.message());
        assertEquals("disabled", response.value().get("databaseMode"));
        assertEquals("", response.value().get("databaseProvider"));
        assertEquals("", response.value().get("databaseResourceId"));
        assertEquals("", response.value().get("databaseEndpoint"));
        assertEquals("false", response.value().get("databaseWriteEnabled"));
        assertEquals("", response.value().get("databaseTables"));
        assertTrue(response.value().get("databaseEvidence").contains("not-provisioned"));
    }

    @Test
    void generatedDaisyBaseBootstrapSqlExecutesAgainstDaisyBaseConnector() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "bootstrap-db",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true")));

        assertTrue(response.success(), response.message());
        InMemoryResourceRepository resources = new InMemoryResourceRepository();
        resources.create(new ResourceRecord(
                response.value().get("databaseResourceId"),
                DaisyBaseProviderCatalog.PROVIDER_ID,
                DaisyBaseProviderCatalog.DATABASE_RESOURCE_TYPE,
                Map.of(
                        "endpoint", response.value().get("databaseEndpoint"),
                        "databaseName", response.value().get("databaseName"),
                        "writeEnabled", response.value().get("databaseWriteEnabled"))));
        DaisyBaseConnector connector = new DaisyBaseConnector(resources);

        for (String sql : response.value().get("databaseBootstrapSql").split(";")) {
            DaisyBaseSqlResult result = connector.execute(response.value().get("databaseResourceId"), sql);
            assertTrue(result.success(), sql + ": " + result.message());
        }

        Map<String, String> databaseAttributes = resources.get(response.value().get("databaseResourceId"))
                .orElseThrow()
                .attributes();
        assertTrue(databaseAttributes.containsKey("daisybase.table.marketplace_items.schema"));
        assertTrue(databaseAttributes.containsKey("daisybase.table.admin_audit_events.schema"));
        assertTrue(databaseAttributes.containsKey("daisybase.table.runtime_health.schema"));
    }

    @Test
    void nodeAgentExecutorBootstrapsDaisyBaseAndStartsRuntimeDriver() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().apply(context(Map.of(
                "serverName", "runtime-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "selectedPlugins", "luckperms")));
        assertTrue(response.success(), response.message());
        InMemoryResourceRepository resources = repositoryWithDatabase(response.value());
        RecordingRuntimeDriver driver = new RecordingRuntimeDriver();

        MinecraftRuntimeExecutionResult result = new MinecraftNodeAgentExecutor(driver, new DaisyBaseConnector(resources))
                .execute("runtime-request-1", response.value());

        assertTrue(result.success(), result.message());
        assertEquals("Succeeded", result.attributes().get("provisioningState"));
        assertEquals("Running", result.attributes().get("observedState"));
        assertEquals("succeeded", result.attributes().get("nodeAgentStatus"));
        assertEquals("healthy", result.attributes().get("healthState"));
        assertTrue(result.attributes().get("runtimeAppliedSteps").contains("prepare-daisybase=bootstrapped:9"));
        assertTrue(result.attributes().get("runtimeAppliedSteps").contains("install-bundled-addons=daisyminecraft:daisy-companion"));
        assertTrue(result.attributes().get("runtimeAppliedSteps").contains("start-container=mc-runtime-node"));
        assertEquals("mc-runtime-node", driver.startedSpec.serviceName());
        assertEquals("primary", driver.startedSpec.activeInstance());
        assertTrue(driver.operations.contains("pull:daisycloud/minecraft-paper:1.21.11"));
        assertTrue(driver.operations.contains("write:mc-runtime-node:server.properties"));
        assertTrue(driver.operations.stream()
                .anyMatch(operation -> operation.startsWith("write-binary:mc-runtime-node:plugins/DaisyCompanion.jar:")));
        assertTrue(driver.operations.contains("bind:mc-runtime-node:25565/tcp=25565"));
        assertTrue(resources.get(response.value().get("databaseResourceId"))
                .orElseThrow()
                .attributes()
                .containsKey("daisybase.table.server_instances.schema"));
    }

    @Test
    void nodeAgentExecutorStopsContainerAndPreservesVolumeOnDelete() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().delete(context(Map.of(
                "serverName", "runtime-delete",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true")));
        assertTrue(response.success(), response.message());
        InMemoryResourceRepository resources = repositoryWithDatabase(response.value());
        RecordingRuntimeDriver driver = new RecordingRuntimeDriver();

        MinecraftRuntimeExecutionResult result = new MinecraftNodeAgentExecutor(driver, new DaisyBaseConnector(resources))
                .execute("runtime-request-delete", response.value());

        assertTrue(result.success(), result.message());
        assertEquals("Deleted", result.attributes().get("provisioningState"));
        assertEquals("Deleted", result.attributes().get("observedState"));
        assertEquals("stopped", result.attributes().get("processState"));
        assertTrue(result.attributes().get("runtimeAppliedSteps").contains("preserve-volume="));
        assertTrue(driver.operations.contains("stop:mc-runtime-delete"));
        assertTrue(driver.operations.contains("release:mc-runtime-delete:25565/tcp=25565"));
    }

    @Test
    void rejectsExternalDatabaseResourceOutsideDaisyBase() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "bad-database",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "databaseMode", "external",
                "databaseName", "bad-database",
                "databaseResourceId", resourceId("not-a-database"))));

        assertFalse(response.success());
        assertEquals("databaseResourceId must target provider DaisyCloud.DaisyBase", response.message());
    }

    @Test
    void plansLiveMarketplaceSourcesAndOneClickInstallPolicy() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "live-market",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "marketplaceMode", "live",
                "marketplaceSources", "modrinth,curseforge,spigotmc",
                "marketplaceInstallPolicy", "one-click-with-preview",
                "marketplaceMalwareScan", "required",
                "marketplaceRollbackPolicy", "snapshot-before-install")));

        assertTrue(response.success(), response.message());
        assertEquals("live", response.value().get("marketplaceMode"));
        assertEquals("modrinth,curseforge,spigotmc", response.value().get("marketplaceSources"));
        assertTrue(response.value().get("marketplaceSearchEndpoints").contains("modrinth=https://api.modrinth.com/v2/search"));
        assertTrue(response.value().get("marketplaceSearchEndpoints").contains("spigotmc=https://api.spiget.org/v2/search/resources"));
        assertFalse(response.value().get("marketplaceSearchEndpoints").contains("offline=embedded-content-catalog"));
        assertEquals("one-click-with-preview", response.value().get("marketplaceInstallPolicy"));
        assertTrue(response.value().get("marketplaceInstallPlan").contains("rollback-on-failure"));
        assertTrue(response.value().get("marketplaceDatabaseTables").contains("content_installs"));
    }

    @Test
    void rejectsUnsupportedMarketplaceSource() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "bad-market",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "marketplaceSources", "modrinth,unknown-store")));

        assertFalse(response.success());
        assertTrue(response.message().contains("marketplaceSources must contain only supported sources"));
    }

    @Test
    void rejectsUnsafeInstanceManagerLimits() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "too-many-instances",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "maxInstances", "51")));

        assertFalse(response.success());
        assertEquals("maxInstances must be between 1 and 50", response.message());
    }

    @Test
    void disabledAdminPanelRemovesRoutesAndPermissions() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "panel-off",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "adminPanel", "disabled")));

        assertTrue(response.success(), response.message());
        assertEquals("disabled", response.value().get("adminPanelState"));
        assertEquals("", response.value().get("adminPanelUrl"));
        assertEquals("", response.value().get("adminPanelRoutes"));
        assertEquals("", response.value().get("adminPanelPermissionTiers"));
        assertEquals("0", response.value().get("adminPanelFeatureCount"));
        assertTrue(response.value().get("adminPanelSecurityPolicy").contains("access=internal"));
    }

    @Test
    void privateNetworkUsesInternalRouteBindingAndPrivateEndpoint() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "private-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "networkMode", "private",
                "fileAccess", "both")));

        assertTrue(response.success(), response.message());
        assertEquals("game=private;panel=internal", response.value().get("networkExposure"));
        assertTrue(response.value().get("networkGameEndpoint").contains("private-node.local.private.mc.internal:25565"));
        assertTrue(response.value().get("networkDaisyNetworkBinding").contains("internal-route-binding:private-node"));
        assertTrue(response.value().get("networkFirewallRules").contains("sftp=allow tcp/22"));
        assertEquals("advanced;edge-scrubbing=always-on", response.value().get("networkDdosPolicy"));
    }

    @Test
    void disabledNetworkCannotExposePublicPanel() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "bad-network",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "networkMode", "disabled",
                "panelAccess", "public")));

        assertFalse(response.success());
        assertEquals("disabled networkMode cannot expose a public admin panel", response.message());
    }

    @Test
    void observeReturnsPendingHealthEvidenceBeforeNodeAgentRuns() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().observe(context(Map.of(
                "serverName", "observe-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true")));

        assertTrue(response.success(), response.message());
        assertEquals("NodeAgentPending", response.value().get("observedState"));
        assertEquals("not-started", response.value().get("processState"));
        assertEquals("unknown-until-node-agent-applies", response.value().get("healthState"));
        assertTrue(response.value().get("containerHealthChecks").contains("tps="));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("verify-health:"));
    }

    @Test
    void nodeAgentIdempotencyKeyIsStableAcrossRequestRetries() {
        Map<String, String> attributes = Map.of(
                "serverName", "retry-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "selectedPlugins", "luckperms");

        ProviderResponse<Map<String, String>> first = new MinecraftServerProvider().apply(context("request-a", attributes));
        ProviderResponse<Map<String, String>> second = new MinecraftServerProvider().apply(context("request-b", attributes));

        assertTrue(first.success(), first.message());
        assertTrue(second.success(), second.message());
        assertEquals(first.value().get("nodeAgentIdempotencyKey"), second.value().get("nodeAgentIdempotencyKey"));
        assertEquals(first.value().get("nodeAgentReconcileDigest"), second.value().get("nodeAgentReconcileDigest"));
        assertNotEquals(first.value().get("nodeAgentTaskId"), second.value().get("nodeAgentTaskId"));
    }

    @Test
    void nodeAgentReconcileDigestChangesWhenStartupFilesChange() {
        ProviderResponse<Map<String, String>> first = new MinecraftServerProvider().apply(context(Map.of(
                "serverName", "digest-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "motd", "First MOTD")));
        ProviderResponse<Map<String, String>> second = new MinecraftServerProvider().apply(context(Map.of(
                "serverName", "digest-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "motd", "Second MOTD")));

        assertTrue(first.success(), first.message());
        assertTrue(second.success(), second.message());
        assertNotEquals(first.value().get("nodeAgentReconcileDigest"), second.value().get("nodeAgentReconcileDigest"));
        assertNotEquals(first.value().get("nodeAgentStartupFileDigests"), second.value().get("nodeAgentStartupFileDigests"));
    }

    @Test
    void deletePlansNodeAgentStopTaskAndPreservesWorldVolume() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().delete(context(Map.of(
                "serverName", "delete-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true")));

        assertTrue(response.success(), response.message());
        assertEquals("Deleting", response.value().get("provisioningState"));
        assertEquals("NodeAgentPendingDelete", response.value().get("observedState"));
        assertEquals("delete-container-preserve-volume", response.value().get("nodeAgentAction"));
        assertEquals("container-removed-data-preserved", response.value().get("nodeAgentDesiredState"));
        assertEquals("preserve-world-volume", response.value().get("deleteMode"));
        assertEquals("true", response.value().get("deleteRequiresBackupCheck"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("preserve-volume:"));
        assertTrue(response.value().get("nodeAgentExecutionPlan").contains("mark-container-deleted:mc-delete-node"));
    }

    @Test
    void acceptsModrinthFabricModSelection() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "fabric-lab",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true",
                "selectedMods", "modrinth:sodium,modrinth:lithium",
                "modSource", "modrinth")));

        assertTrue(response.success(), response.message());
        assertEquals("fabric", response.value().get("modLoader"));
        assertEquals("modrinth:sodium,modrinth:lithium", response.value().get("selectedMods"));
        assertEquals("4096", response.value().get("memoryMb"));
        assertEquals("2", response.value().get("contentLockItemCount"));
        assertEquals("offline-metadata", response.value().get("contentResolverMode"));
        assertTrue(response.value().get("contentLockDigest").matches("[a-f0-9]{64}"));
        assertTrue(response.value().get("contentLockSummary").contains("mods=2"));
        assertTrue(response.value().get("compatibilityReport").contains("2 mods"));
    }

    @Test
    void contentLockJsonContainsStructuredModItems() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().apply(context(Map.of(
                "serverName", "content-node",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true",
                "selectedMods", "modrinth:sodium,curseforge:lithium",
                "modSource", "modrinth")));

        assertTrue(response.success(), response.message());
        String contentLock = response.value().get("startupFile:content-lock.json");
        assertTrue(contentLock.contains("\"schemaVersion\": \"daisyminecraft.content.v1\""));
        assertTrue(contentLock.contains("\"contentDigest\": \"" + response.value().get("contentLockDigest") + "\""));
        assertTrue(contentLock.contains("\"kind\":\"mod\""));
        assertTrue(contentLock.contains("\"source\":\"modrinth\""));
        assertTrue(contentLock.contains("\"id\":\"sodium\""));
        assertTrue(contentLock.contains("\"source\":\"curseforge\""));
        assertTrue(contentLock.contains("\"id\":\"lithium\""));
        assertTrue(contentLock.contains("\"loader\":\"fabric\""));
    }

    @Test
    void contentLockDigestIsStableForEquivalentSelections() {
        Map<String, String> attributes = Map.of(
                "serverName", "stable-lock",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true",
                "selectedMods", "modrinth:sodium,modrinth:lithium",
                "modSource", "modrinth");

        ProviderResponse<Map<String, String>> first = new MinecraftServerProvider().plan(context(attributes));
        ProviderResponse<Map<String, String>> second = new MinecraftServerProvider().plan(context(attributes));

        assertTrue(first.success(), first.message());
        assertTrue(second.success(), second.message());
        assertEquals(first.value().get("contentLockDigest"), second.value().get("contentLockDigest"));
        assertEquals(first.value().get("contentLockSummary"), second.value().get("contentLockSummary"));
    }

    @Test
    void offlineCatalogExpandsKnownDependenciesIntoContentLock() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().apply(context(Map.of(
                "serverName", "dependency-node",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true",
                "selectedMods", "modrinth:sodium-extra",
                "modSource", "modrinth")));

        assertTrue(response.success(), response.message());
        assertEquals("2", response.value().get("contentLockItemCount"));
        assertEquals("0", response.value().get("contentLockWarningCount"));
        String contentLock = response.value().get("startupFile:content-lock.json");
        assertTrue(contentLock.contains("\"id\":\"sodium-extra\""));
        assertTrue(contentLock.contains("\"id\":\"sodium\""));
        assertTrue(contentLock.contains("\"resolverMode\": \"offline-metadata\""));
    }

    @Test
    void offlineCatalogWarnsWhenMetadataIsMissing() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "unknown-content",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true",
                "selectedMods", "modrinth:unknown-mod",
                "modSource", "modrinth")));

        assertTrue(response.success(), response.message());
        assertEquals("1", response.value().get("contentLockWarningCount"));
        assertTrue(response.value().get("contentWarnings").contains("metadata unavailable"));
        assertTrue(response.value().get("contentLockDigest").matches("[a-f0-9]{64}"));
    }

    @Test
    void offlineCatalogRejectsKnownLoaderMismatch() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "forge-sodium",
                "minecraftVersion", "1.21.11",
                "serverType", "forge",
                "eulaAccepted", "true",
                "selectedMods", "modrinth:sodium",
                "modSource", "modrinth")));

        assertFalse(response.success());
        assertEquals("modrinth:sodium does not support mod loader forge", response.message());
    }

    @Test
    void rejectsModsOnPaperServer() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "paper-mods",
                "minecraftVersion", "1.21.11",
                "serverType", "paper",
                "eulaAccepted", "true",
                "selectedMods", "sodium")));

        assertFalse(response.success());
        assertEquals("selectedMods require a Forge, Fabric, Quilt, or NeoForge server type", response.message());
    }

    @Test
    void acceptsPluginsOnPaperServer() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "paper-hub",
                "minecraftVersion", "1.21.11",
                "serverType", "paper",
                "eulaAccepted", "true",
                "selectedPlugins", "luckperms,geyser")));

        assertTrue(response.success(), response.message());
        assertEquals("luckperms,geyser", response.value().get("selectedPlugins"));
        assertTrue(response.value().get("compatibilityReport").contains("2 plugins"));
    }

    @Test
    void defaultsDaisyCompanionOffForNonPluginServerTypes() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "fabric-hub",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true")));

        assertTrue(response.success(), response.message());
        assertEquals("disabled", response.value().get("daisyCompanion"));
        assertEquals("0", response.value().get("bundledAddonCount"));
        assertEquals("disabled", response.value().get("bundledAddonPlan"));
    }

    @Test
    void rejectsDaisyCompanionOnNonPluginServerTypesWhenExplicitlyEnabled() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "fabric-daisy",
                "minecraftVersion", "1.21.11",
                "serverType", "fabric",
                "eulaAccepted", "true",
                "daisyCompanion", "enabled")));

        assertFalse(response.success());
        assertEquals("daisyCompanion=enabled requires serverType paper, spigot, or purpur", response.message());
    }

    @Test
    void rejectsPublicAdminPanelWithoutTwoFactor() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "unsafe-panel",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "panelAccess", "public",
                "twoFactorRequired", "false")));

        assertFalse(response.success());
        assertEquals("public admin panels must require twoFactorRequired=true", response.message());
    }

    @Test
    void rejectsMissingEulaAcceptance() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "no-eula",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "false")));

        assertFalse(response.success());
        assertEquals("eulaAccepted must be true before provisioning a Minecraft server", response.message());
    }

    @Test
    void rejectsPublicEndpointWithoutDdosProtection() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "public-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "networkMode", "public",
                "ddosProtection", "none")));

        assertFalse(response.success());
        assertEquals("public Minecraft endpoints must enable DDoS protection", response.message());
    }

    @Test
    void rejectsInvalidStartupTuningValues() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "bad-tuning",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "viewDistance", "64")));

        assertFalse(response.success());
        assertEquals("viewDistance must be between 2 and 32", response.message());
    }

    @Test
    void backupRestoreAndDiagnoseReturnOperationalEvidence() {
        MinecraftServerProvider provider = new MinecraftServerProvider();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("serverName", "ops-node");
        attributes.put("minecraftVersion", "1.21.11");
        attributes.put("eulaAccepted", "true");

        ProviderResponse<Map<String, String>> backup = provider.backup(context(attributes));
        ProviderResponse<Map<String, String>> restore = provider.restore(context(attributes));
        ProviderResponse<Map<String, String>> diagnose = provider.diagnose(context(attributes));

        assertTrue(backup.success(), backup.message());
        assertEquals("true", backup.value().get("backupRestorable"));
        assertEquals("daisyminecraft.backup.v1", backup.value().get("backupPolicySchema"));
        assertTrue(backup.value().get("backupScope").contains("node-agent-task"));
        assertTrue(restore.success(), restore.message());
        assertEquals("true", restore.value().get("restoreRequiresStop"));
        assertTrue(diagnose.success(), diagnose.message());
        assertTrue(diagnose.value().get("healthSignals").contains("tps"));
        assertTrue(diagnose.value().get("supportBundle").contains("crash-reports"));
    }

    @Test
    void acceptsReplicatedCompressedBackupPolicy() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().plan(context(Map.of(
                "serverName", "backup-node",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "backupSchedule", "hourly",
                "backupRetentionDays", "30",
                "backupStorage", "replicated",
                "backupCompression", "gzip",
                "selectedPlugins", "luckperms")));

        assertTrue(response.success(), response.message());
        assertEquals("hourly/30d", response.value().get("backupPolicy"));
        assertEquals("replicated", response.value().get("backupStorage"));
        assertEquals("gzip", response.value().get("backupCompression"));
        assertEquals("PT1H", response.value().get("backupRecoveryPointObjective"));
        assertEquals("enabled:replicated", response.value().get("backupOffsitePolicy"));
        assertTrue(response.value().get("backupScope").contains("plugins,content-lock"));
        assertTrue(response.value().get("backupPolicyEvidence").contains("retentionDays=30"));
    }

    @Test
    void rejectsUnsupportedBackupStorage() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().validate(context(Map.of(
                "serverName", "bad-backup",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "backupStorage", "usb-drive")));

        assertFalse(response.success());
        assertTrue(response.message().contains("backupStorage must be one of"));
    }

    @Test
    void importAndExportStateReturnMigrationContracts() {
        MinecraftServerProvider provider = new MinecraftServerProvider();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("serverName", "migration-node");
        attributes.put("minecraftVersion", "1.21.11");
        attributes.put("eulaAccepted", "true");
        attributes.put("selectedPlugins", "luckperms");
        attributes.put("importMode", "full-server");
        attributes.put("importSource", "apex-export-2026-04-25.zip");

        ProviderResponse<Map<String, String>> imported = provider.importState(context(attributes));
        ProviderResponse<Map<String, String>> exported = provider.exportState(context(attributes));

        assertTrue(imported.success(), imported.message());
        assertEquals("ImportPending", imported.value().get("provisioningState"));
        assertEquals("full-server", imported.value().get("importMode"));
        assertTrue(imported.value().get("importScope").contains("admin-panel-profile"));
        assertEquals("verify-selected-content", imported.value().get("importContentLockAction"));
        assertEquals("true", imported.value().get("importRequiresBackupBeforeApply"));
        assertTrue(exported.success(), exported.message());
        assertEquals("daisyminecraft.export.v1", exported.value().get("exportSchema"));
        assertEquals("true", exported.value().get("exportRestorable"));
        assertTrue(exported.value().get("exportScope").contains("content-lock"));
        assertEquals(exported.value().get("contentLockDigest"), exported.value().get("exportContentLockDigest"));
        assertEquals("true", exported.value().get("exportIncludesAdminPanelProfile"));
    }

    @Test
    void rejectsUnsupportedImportMode() {
        ProviderResponse<Map<String, String>> response = new MinecraftServerProvider().importState(context(Map.of(
                "serverName", "bad-import",
                "minecraftVersion", "1.21.11",
                "eulaAccepted", "true",
                "importMode", "raw-rootfs")));

        assertFalse(response.success());
        assertTrue(response.message().contains("importMode must be one of"));
    }

    private static InMemoryResourceRepository repositoryWithDatabase(Map<String, String> attributes) {
        InMemoryResourceRepository resources = new InMemoryResourceRepository();
        resources.create(new ResourceRecord(
                attributes.get("databaseResourceId"),
                DaisyBaseProviderCatalog.PROVIDER_ID,
                DaisyBaseProviderCatalog.DATABASE_RESOURCE_TYPE,
                Map.of(
                        "endpoint", attributes.get("databaseEndpoint"),
                        "databaseName", attributes.get("databaseName"),
                        "writeEnabled", attributes.get("databaseWriteEnabled"))));
        return resources;
    }

    private static final class RecordingRuntimeDriver implements MinecraftRuntimeDriver {
        private final List<String> operations = new ArrayList<>();
        private MinecraftRuntimeContainerSpec startedSpec;

        @Override
        public void pullImage(String image) {
            operations.add("pull:" + image);
        }

        @Override
        public void ensureVolume(String volumeName, String mountPath) {
            operations.add("volume:" + volumeName + "=" + mountPath);
        }

        @Override
        public void writeStartupFile(String serviceName, String fileName, String content) {
            operations.add("write:" + serviceName + ":" + fileName);
        }

        @Override
        public void writeBinaryFile(String serviceName, String fileName, byte[] content) {
            operations.add("write-binary:" + serviceName + ":" + fileName + ":" + content.length);
        }

        @Override
        public void bindPort(String serviceName, String containerPort, String hostPort) {
            operations.add("bind:" + serviceName + ":" + containerPort + "=" + hostPort);
        }

        @Override
        public void startContainer(MinecraftRuntimeContainerSpec spec) {
            startedSpec = spec;
            operations.add("start:" + spec.serviceName());
        }

        @Override
        public void stopContainer(String serviceName) {
            operations.add("stop:" + serviceName);
        }

        @Override
        public void releasePort(String serviceName, String containerPort, String hostPort) {
            operations.add("release:" + serviceName + ":" + containerPort + "=" + hostPort);
        }

        @Override
        public Map<String, String> probeHealth(String serviceName, Map<String, String> expectedChecks) {
            Map<String, String> health = new LinkedHashMap<>();
            expectedChecks.keySet().forEach(check -> health.put(check, "healthy"));
            operations.add("probe:" + serviceName + ":" + String.join(",", expectedChecks.keySet()));
            return health;
        }
    }

    private static ProviderRequestContext context(Map<String, String> attributes) {
        return context("request-1", attributes);
    }

    private static ProviderRequestContext context(String requestId, Map<String, String> attributes) {
        return new ProviderRequestContext(
                requestId,
                ProviderOperationKind.PLAN,
                MinecraftServerProviderCatalog.PROVIDER_ID,
                MinecraftServerProviderCatalog.SERVER_RESOURCE_TYPE,
                resourceId("survival-core"),
                attributes);
    }

    private static String resourceId(String name) {
        return "/subscriptions/11111111-1111-4111-8111-111111111111/resourceGroups/rg-test/providers/"
                + MinecraftServerProviderCatalog.PROVIDER_ID
                + "/"
                + MinecraftServerProviderCatalog.SERVER_RESOURCE_TYPE
                + "/"
                + name;
    }
}

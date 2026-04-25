package dev.daisycloud.provider.minecraft;

import dev.daisycloud.model.ResourceId;
import dev.daisycloud.model.ResourceTypeName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MinecraftDatabaseProfile(
        String mode,
        String providerId,
        String resourceType,
        String resourceId,
        String databaseName,
        String endpoint,
        boolean writeEnabled,
        String sku,
        String schemaVersion,
        List<String> tables,
        List<String> bootstrapSql,
        String evidence) {
    static final String DAISYBASE_PROVIDER_ID = "DaisyCloud.DaisyBase";
    static final String DAISYBASE_DATABASE_RESOURCE_TYPE = "databases";

    private static final List<String> CONTROL_PLANE_TABLES = List.of(
            "marketplace_items",
            "content_installs",
            "admin_audit_events",
            "server_instances",
            "instance_snapshots",
            "player_actions",
            "console_events",
            "runtime_health",
            "rollback_points");

    public MinecraftDatabaseProfile {
        mode = requireText(mode, "mode");
        providerId = Objects.requireNonNull(providerId, "providerId must not be null").trim();
        resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null").trim();
        resourceId = Objects.requireNonNull(resourceId, "resourceId must not be null").trim();
        databaseName = Objects.requireNonNull(databaseName, "databaseName must not be null").trim();
        endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null").trim();
        sku = Objects.requireNonNull(sku, "sku must not be null").trim();
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        tables = List.copyOf(Objects.requireNonNull(tables, "tables must not be null"));
        bootstrapSql = List.copyOf(Objects.requireNonNull(bootstrapSql, "bootstrapSql must not be null"));
        evidence = requireText(evidence, "evidence");
    }

    public static MinecraftDatabaseProfile fromPlannedAttributes(
            Map<String, String> attributes,
            String minecraftResourceId) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String mode = requirePlanned(planned, "databaseMode");
        if ("disabled".equals(mode)) {
            return new MinecraftDatabaseProfile(
                    mode,
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    "",
                    "daisyminecraft.database.v1",
                    List.of(),
                    List.of(),
                    "databaseMode=disabled;daisyBase=not-provisioned");
        }

        String databaseName = requirePlanned(planned, "databaseName");
        String endpoint = requirePlanned(planned, "databaseEndpoint");
        if (!endpoint.startsWith("jdbc:daisybase://")) {
            throw new IllegalArgumentException("databaseEndpoint must use jdbc:daisybase://");
        }
        String sku = requirePlanned(planned, "databaseSku");
        String configuredResourceId = planned.getOrDefault("databaseResourceId", "").trim();
        String resourceId = "managed".equals(mode) && configuredResourceId.isBlank()
                ? defaultDatabaseResourceId(minecraftResourceId, databaseName)
                : configuredResourceId;
        if (resourceId.isBlank()) {
            throw new IllegalArgumentException("databaseResourceId is required when databaseMode=" + mode);
        }
        validateDaisyBaseResourceId(resourceId);

        return new MinecraftDatabaseProfile(
                mode,
                DAISYBASE_PROVIDER_ID,
                DAISYBASE_DATABASE_RESOURCE_TYPE,
                resourceId,
                databaseName,
                endpoint,
                true,
                sku,
                "daisyminecraft.database.v1",
                CONTROL_PLANE_TABLES,
                bootstrapSqlStatements(),
                "databaseMode=" + mode
                        + ";provider=" + DAISYBASE_PROVIDER_ID
                        + ";tables=" + CONTROL_PLANE_TABLES.size()
                        + ";schema=daisyminecraft.database.v1");
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("databaseProvider", providerId);
        attributes.put("databaseResourceType", resourceType);
        attributes.put("databaseResourceId", resourceId);
        attributes.put("databaseName", databaseName);
        attributes.put("databaseEndpoint", endpoint);
        attributes.put("databaseWriteEnabled", Boolean.toString(writeEnabled));
        attributes.put("databaseSku", sku);
        attributes.put("databaseSchemaVersion", schemaVersion);
        attributes.put("databaseTables", String.join(",", tables));
        attributes.put("databaseBootstrapSql", String.join(";", bootstrapSql));
        attributes.put("databaseEvidence", evidence);
        return Map.copyOf(attributes);
    }

    private static String defaultDatabaseResourceId(String minecraftResourceId, String databaseName) {
        String sourceResourceId = Objects.requireNonNull(minecraftResourceId, "resourceId must not be null").trim();
        if (sourceResourceId.isEmpty()) {
            throw new IllegalArgumentException("resourceId is required to derive managed DaisyBase databaseResourceId");
        }
        ResourceId parsed = ResourceId.parse(sourceResourceId);
        if (parsed.isManagementGroup()) {
            throw new IllegalArgumentException("managed DaisyBase databaseResourceId requires a subscription resourceId");
        }
        return "/subscriptions/" + parsed.subscriptionId()
                + "/resourceGroups/" + parsed.resourceGroup()
                + "/providers/" + DAISYBASE_PROVIDER_ID
                + "/" + DAISYBASE_DATABASE_RESOURCE_TYPE
                + "/" + databaseName;
    }

    private static void validateDaisyBaseResourceId(String value) {
        ResourceId parsed = ResourceId.parse(value);
        if (!DAISYBASE_PROVIDER_ID.equals(parsed.providerNamespace())) {
            throw new IllegalArgumentException("databaseResourceId must target provider " + DAISYBASE_PROVIDER_ID);
        }
        List<ResourceTypeName> pairs = parsed.resourceTypeNames();
        if (pairs.isEmpty() || !DAISYBASE_DATABASE_RESOURCE_TYPE.equals(pairs.get(pairs.size() - 1).type())) {
            throw new IllegalArgumentException("databaseResourceId must target resource type " + DAISYBASE_DATABASE_RESOURCE_TYPE);
        }
    }

    private static List<String> bootstrapSqlStatements() {
        return List.of(
                "CREATE TABLE marketplace_items (id TEXT, source TEXT, project_type TEXT, loader TEXT, version TEXT, trust_level TEXT)",
                "CREATE TABLE content_installs (install_id TEXT, content_id TEXT, source TEXT, version TEXT, status TEXT, installed_at TEXT)",
                "CREATE TABLE admin_audit_events (event_id TEXT, actor TEXT, action TEXT, resource TEXT, created_at TEXT)",
                "CREATE TABLE server_instances (instance_id TEXT, profile_id TEXT, world_name TEXT, status TEXT, active TEXT)",
                "CREATE TABLE instance_snapshots (snapshot_id TEXT, instance_id TEXT, reason TEXT, created_at TEXT, restorable TEXT)",
                "CREATE TABLE player_actions (action_id TEXT, player TEXT, action TEXT, actor TEXT, created_at TEXT)",
                "CREATE TABLE console_events (event_id TEXT, stream TEXT, severity TEXT, message TEXT, created_at TEXT)",
                "CREATE TABLE runtime_health (sample_id TEXT, instance_id TEXT, signal TEXT, value TEXT, created_at TEXT)",
                "CREATE TABLE rollback_points (rollback_id TEXT, install_id TEXT, snapshot_id TEXT, status TEXT, created_at TEXT)");
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

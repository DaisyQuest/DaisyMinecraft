package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MinecraftBackupPolicyProfile(
        String schemaVersion,
        String schedule,
        int retentionDays,
        String storage,
        String compression,
        boolean backupBeforeDestructiveChanges,
        String scope,
        String restoreModes,
        String recoveryPointObjective,
        String evidence) {
    public MinecraftBackupPolicyProfile {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        schedule = requireText(schedule, "schedule");
        if (retentionDays < 0) {
            throw new IllegalArgumentException("retentionDays must not be negative");
        }
        storage = requireText(storage, "storage");
        compression = requireText(compression, "compression");
        scope = requireText(scope, "scope");
        restoreModes = requireText(restoreModes, "restoreModes");
        recoveryPointObjective = requireText(recoveryPointObjective, "recoveryPointObjective");
        evidence = requireText(evidence, "evidence");
    }

    public static MinecraftBackupPolicyProfile fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String schedule = requirePlanned(planned, "backupSchedule");
        int retentionDays = Integer.parseInt(requirePlanned(planned, "backupRetentionDays"));
        String storage = requirePlanned(planned, "backupStorage");
        String compression = requirePlanned(planned, "backupCompression");
        boolean beforeDestructive = Boolean.parseBoolean(requirePlanned(planned, "backupBeforeDestructiveChanges"));
        String scope = contentScope(planned);
        return new MinecraftBackupPolicyProfile(
                "daisyminecraft.backup.v1",
                schedule,
                retentionDays,
                storage,
                compression,
                beforeDestructive,
                scope,
                "replace-world-keep-panel-policy,restore-world-only,restore-full-server-preview",
                recoveryPointObjective(schedule),
                evidence(schedule, retentionDays, storage, beforeDestructive));
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("backupPolicySchema", schemaVersion);
        attributes.put("backupScope", scope);
        attributes.put("backupStorage", storage);
        attributes.put("backupCompression", compression);
        attributes.put("backupBeforeDestructiveChanges", Boolean.toString(backupBeforeDestructiveChanges));
        attributes.put("backupRestoreModes", restoreModes);
        attributes.put("backupRecoveryPointObjective", recoveryPointObjective);
        attributes.put("backupOffsitePolicy", "local".equals(storage) ? "disabled" : "enabled:" + storage);
        attributes.put("backupPolicyEvidence", evidence);
        return Map.copyOf(attributes);
    }

    private static String contentScope(Map<String, String> planned) {
        StringBuilder scope = new StringBuilder("world,config,server-properties");
        if (!planned.getOrDefault("selectedMods", "").isBlank() || !planned.getOrDefault("modpackId", "").isBlank()) {
            scope.append(",mods,content-lock");
        }
        if (!planned.getOrDefault("selectedPlugins", "").isBlank()) {
            scope.append(",plugins,content-lock");
        }
        scope.append(",admin-panel-profile,node-agent-task");
        return scope.toString();
    }

    private static String recoveryPointObjective(String schedule) {
        return switch (schedule) {
            case "hourly" -> "PT1H";
            case "daily" -> "P1D";
            case "weekly" -> "P7D";
            case "none" -> "manual-only";
            default -> "unknown";
        };
    }

    private static String evidence(
            String schedule,
            int retentionDays,
            String storage,
            boolean beforeDestructive) {
        return "schedule=" + schedule
                + ";retentionDays=" + retentionDays
                + ";storage=" + storage
                + ";backupBeforeDestructiveChanges=" + beforeDestructive;
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

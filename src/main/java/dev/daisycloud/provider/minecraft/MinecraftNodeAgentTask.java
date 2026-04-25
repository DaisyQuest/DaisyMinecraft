package dev.daisycloud.provider.minecraft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MinecraftNodeAgentTask(
        String schemaVersion,
        String taskId,
        String action,
        String desiredState,
        String idempotencyKey,
        String reconcileDigest,
        MinecraftContainerManifest manifest,
        Map<String, String> startupFileDigests,
        List<String> executionSteps,
        Map<String, String> evidence) {
    public MinecraftNodeAgentTask {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        taskId = requireText(taskId, "taskId");
        action = requireText(action, "action");
        desiredState = requireText(desiredState, "desiredState");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        reconcileDigest = requireText(reconcileDigest, "reconcileDigest");
        manifest = Objects.requireNonNull(manifest, "manifest must not be null");
        startupFileDigests = copyMap(startupFileDigests, "startupFileDigests");
        executionSteps = List.copyOf(Objects.requireNonNull(executionSteps, "executionSteps must not be null"));
        if (executionSteps.isEmpty()) {
            throw new IllegalArgumentException("executionSteps must not be empty");
        }
        evidence = copyMap(evidence, "evidence");
    }

    public static MinecraftNodeAgentTask createOrReconcile(
            String requestId,
            Map<String, String> planned,
            MinecraftContainerManifest manifest) {
        List<String> steps = List.of(
                "pull-image:" + manifest.image(),
                "ensure-volume:" + joinMap(manifest.volumes()),
                "prepare-daisybase:" + planned.getOrDefault("databaseResourceId", "disabled"),
                "resolve-marketplace-content:" + planned.getOrDefault("marketplaceMode", "offline"),
                "write-startup-files:" + String.join(",", manifest.startupFiles().files().keySet()),
                "install-bundled-addons:" + planned.getOrDefault("bundledAddonPlan", "disabled"),
                "bind-ports:" + joinMap(manifest.portBindings()),
                "activate-instance:" + planned.getOrDefault("activeInstance", "primary"),
                "start-container:" + manifest.serviceName(),
                "verify-health:" + String.join(",", manifest.healthChecks().keySet()));
        return task(
                requestId,
                planned,
                manifest,
                "create-or-reconcile-container",
                "running",
                steps,
                "create/update handoff accepted for node-agent reconciliation");
    }

    public static MinecraftNodeAgentTask delete(
            String requestId,
            Map<String, String> planned,
            MinecraftContainerManifest manifest) {
        List<String> steps = List.of(
                "stop-container:" + manifest.serviceName(),
                "preserve-volume:" + joinMap(manifest.volumes()),
                "release-ports:" + joinMap(manifest.portBindings()),
                "mark-container-deleted:" + manifest.serviceName());
        return task(
                requestId,
                planned,
                manifest,
                "delete-container-preserve-volume",
                "container-removed-data-preserved",
                steps,
                "delete handoff preserves world volume and backup evidence");
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("nodeAgentTaskSchema", schemaVersion);
        attributes.put("nodeAgentTaskId", taskId);
        attributes.put("nodeAgentAction", action);
        attributes.put("nodeAgentDesiredState", desiredState);
        attributes.put("nodeAgentIdempotencyKey", idempotencyKey);
        attributes.put("nodeAgentReconcileDigest", reconcileDigest);
        attributes.put("nodeAgentExecutionPlan", String.join(";", executionSteps));
        attributes.put("nodeAgentStartupFileDigests", joinMap(startupFileDigests));
        attributes.put("nodeAgentEvidence", joinMap(evidence));
        attributes.put("nodeAgentDispatchState", "pending");
        attributes.put("nodeAgentStatus", "queued");
        attributes.put("nodeAgentStatusReason", "waiting-for-node-agent");
        attributes.put("nodeAgentStatusEndpoint", "/node-agents/tasks/" + taskId);
        attributes.put("containerApplyState", "planned-for-node-agent");
        return Map.copyOf(attributes);
    }

    private static MinecraftNodeAgentTask task(
            String requestId,
            Map<String, String> planned,
            MinecraftContainerManifest manifest,
            String action,
            String desiredState,
            List<String> steps,
            String handoffSummary) {
        requireText(requestId, "requestId");
        Map<String, String> startupFileDigests = startupFileDigests(manifest);
        String reconcileDigest = reconcileDigest(action, desiredState, manifest, startupFileDigests, steps);
        String idempotencyKey = action + ":" + manifest.serviceName() + ":" + reconcileDigest;
        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("requestId", requestId);
        evidence.put("resourceId", planned.getOrDefault("resourceId", ""));
        evidence.put("serviceName", manifest.serviceName());
        evidence.put("image", manifest.image());
        evidence.put("contentLockDigest", planned.getOrDefault("contentLockDigest", ""));
        evidence.put("databaseResourceId", planned.getOrDefault("databaseResourceId", ""));
        evidence.put("marketplaceMode", planned.getOrDefault("marketplaceMode", ""));
        evidence.put("activeInstance", planned.getOrDefault("activeInstance", ""));
        evidence.put("adminPanelUxSchema", planned.getOrDefault("adminPanelUxSchema", ""));
        evidence.put("startupFileCount", Integer.toString(manifest.startupFiles().files().size()));
        evidence.put("portBindingCount", Integer.toString(manifest.portBindings().size()));
        evidence.put("volumeCount", Integer.toString(manifest.volumes().size()));
        evidence.put("handoffSummary", handoffSummary);
        return new MinecraftNodeAgentTask(
                "daisyminecraft.node-agent-task.v1",
                "minecraft-node-agent-" + manifest.serviceName() + "-" + sha256(requestId + "\n" + idempotencyKey).substring(0, 12),
                action,
                desiredState,
                idempotencyKey,
                reconcileDigest,
                manifest,
                startupFileDigests,
                steps,
                evidence);
    }

    private static Map<String, String> startupFileDigests(MinecraftContainerManifest manifest) {
        Map<String, String> digests = new LinkedHashMap<>();
        manifest.startupFiles().files().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> digests.put(entry.getKey(), sha256(entry.getValue())));
        return digests;
    }

    private static String reconcileDigest(
            String action,
            String desiredState,
            MinecraftContainerManifest manifest,
            Map<String, String> startupFileDigests,
            List<String> steps) {
        return sha256(action + "\n"
                + desiredState + "\n"
                + manifest.manifestVersion() + "\n"
                + manifest.serviceName() + "\n"
                + manifest.image() + "\n"
                + manifest.command() + "\n"
                + joinMap(manifest.environment()) + "\n"
                + joinMap(manifest.portBindings()) + "\n"
                + joinMap(manifest.volumes()) + "\n"
                + joinMap(manifest.resourceLimits()) + "\n"
                + manifest.restartPolicy() + "\n"
                + joinMap(startupFileDigests) + "\n"
                + String.join("\n", steps));
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
        values.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    if (builder.length() > 0) {
                        builder.append(';');
                    }
                    builder.append(entry.getKey()).append('=').append(entry.getValue());
                });
        return builder.toString();
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 digest is unavailable", error);
        }
    }
}

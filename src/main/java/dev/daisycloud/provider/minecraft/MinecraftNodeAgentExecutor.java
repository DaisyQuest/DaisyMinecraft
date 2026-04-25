package dev.daisycloud.provider.minecraft;

import dev.daisycloud.provider.daisybase.DaisyBaseConnectionResult;
import dev.daisycloud.provider.daisybase.DaisyBaseConnector;
import dev.daisycloud.provider.daisybase.DaisyBaseSqlResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MinecraftNodeAgentExecutor {
    private final MinecraftRuntimeDriver runtimeDriver;
    private final DaisyBaseConnector daisyBaseConnector;

    public MinecraftNodeAgentExecutor(MinecraftRuntimeDriver runtimeDriver, DaisyBaseConnector daisyBaseConnector) {
        this.runtimeDriver = Objects.requireNonNull(runtimeDriver, "runtimeDriver must not be null");
        this.daisyBaseConnector = Objects.requireNonNull(daisyBaseConnector, "daisyBaseConnector must not be null");
    }

    public MinecraftRuntimeExecutionResult execute(String executionRequestId, Map<String, String> attributes) {
        requireText(executionRequestId, "executionRequestId");
        Map<String, String> source = new LinkedHashMap<>(Objects.requireNonNull(attributes, "attributes must not be null"));
        try {
            String action = require(source, "nodeAgentAction");
            if ("create-or-reconcile-container".equals(action)) {
                return reconcile(executionRequestId, source);
            }
            if ("delete-container-preserve-volume".equals(action)) {
                return delete(executionRequestId, source);
            }
            return failure(source, "Unsupported Minecraft node-agent action: " + action);
        } catch (RuntimeException error) {
            return failure(source, error.getMessage());
        }
    }

    private MinecraftRuntimeExecutionResult reconcile(String executionRequestId, Map<String, String> attributes) {
        String serviceName = require(attributes, "containerServiceName");
        Map<String, String> portBindings = parseMap(require(attributes, "containerPortBindings"));
        Map<String, String> volumes = parseMap(require(attributes, "containerVolumes"));
        Map<String, String> healthChecks = parseMap(require(attributes, "containerHealthChecks"));
        Map<String, String> appliedSteps = new LinkedHashMap<>();

        String databaseState = bootstrapDaisyBase(attributes);
        appliedSteps.put("prepare-daisybase", databaseState);

        runtimeDriver.pullImage(require(attributes, "containerImage"));
        appliedSteps.put("pull-image", require(attributes, "containerImage"));
        for (Map.Entry<String, String> volume : volumes.entrySet()) {
            runtimeDriver.ensureVolume(volume.getKey(), volume.getValue());
        }
        appliedSteps.put("ensure-volume", joinMap(volumes));
        for (Map.Entry<String, String> file : startupFiles(attributes).entrySet()) {
            runtimeDriver.writeStartupFile(serviceName, file.getKey(), file.getValue());
        }
        appliedSteps.put("write-startup-files", require(attributes, "startupFileNames"));
        appliedSteps.put("install-bundled-addons",
                MinecraftBundledAddons.installBundledAddons(runtimeDriver, serviceName, attributes));
        for (Map.Entry<String, String> port : portBindings.entrySet()) {
            runtimeDriver.bindPort(serviceName, port.getKey(), port.getValue());
        }
        appliedSteps.put("bind-ports", joinMap(portBindings));
        MinecraftRuntimeContainerSpec spec = containerSpec(attributes);
        runtimeDriver.startContainer(spec);
        appliedSteps.put("start-container", serviceName);
        Map<String, String> observedHealth = runtimeDriver.probeHealth(serviceName, healthChecks);
        appliedSteps.put("verify-health", joinMap(observedHealth));

        Map<String, String> result = new LinkedHashMap<>(attributes);
        result.put("runtimeExecutionId", executionId(executionRequestId, attributes));
        result.put("runtimeExecutedAt", Instant.now().toString());
        result.put("runtimeAppliedSteps", joinMap(appliedSteps));
        result.put("runtimeHealthSignals", joinMap(observedHealth));
        result.put("runtimeActiveInstance", require(attributes, "activeInstance"));
        result.put("runtimeDatabaseBootstrapState", databaseState);
        result.put("nodeAgentDispatchState", "completed");
        result.put("nodeAgentStatus", "succeeded");
        result.put("nodeAgentStatusReason", "node-agent-applied");
        result.put("containerApplyState", "applied-by-node-agent");
        result.put("provisioningState", "Succeeded");
        result.put("observedState", "Running");
        result.put("processState", "running");
        result.put("healthState", observedHealth.values().stream().allMatch("healthy"::equals) ? "healthy" : "degraded");
        return MinecraftRuntimeExecutionResult.success(Map.copyOf(result), "Minecraft runtime reconciled");
    }

    private MinecraftRuntimeExecutionResult delete(String executionRequestId, Map<String, String> attributes) {
        String serviceName = require(attributes, "containerServiceName");
        Map<String, String> portBindings = parseMap(require(attributes, "containerPortBindings"));
        Map<String, String> appliedSteps = new LinkedHashMap<>();
        runtimeDriver.stopContainer(serviceName);
        appliedSteps.put("stop-container", serviceName);
        for (Map.Entry<String, String> port : portBindings.entrySet()) {
            runtimeDriver.releasePort(serviceName, port.getKey(), port.getValue());
        }
        appliedSteps.put("release-ports", joinMap(portBindings));
        appliedSteps.put("preserve-volume", require(attributes, "containerVolumes"));

        Map<String, String> result = new LinkedHashMap<>(attributes);
        result.put("runtimeExecutionId", executionId(executionRequestId, attributes));
        result.put("runtimeExecutedAt", Instant.now().toString());
        result.put("runtimeAppliedSteps", joinMap(appliedSteps));
        result.put("runtimeActiveInstance", attributes.getOrDefault("activeInstance", "primary"));
        result.put("nodeAgentDispatchState", "completed");
        result.put("nodeAgentStatus", "succeeded");
        result.put("nodeAgentStatusReason", "node-agent-delete-applied");
        result.put("containerApplyState", "deleted-by-node-agent");
        result.put("provisioningState", "Deleted");
        result.put("observedState", "Deleted");
        result.put("processState", "stopped");
        result.put("healthState", "not-applicable");
        return MinecraftRuntimeExecutionResult.success(Map.copyOf(result), "Minecraft runtime deleted with data preserved");
    }

    private String bootstrapDaisyBase(Map<String, String> attributes) {
        if ("disabled".equals(attributes.getOrDefault("databaseMode", "disabled"))) {
            return "skipped:disabled";
        }
        String databaseResourceId = require(attributes, "databaseResourceId");
        DaisyBaseConnectionResult connection = daisyBaseConnector.connect(databaseResourceId);
        if (!connection.connected()) {
            throw new IllegalStateException("DaisyBase connection failed: " + connection.message());
        }
        String sqlText = attributes.getOrDefault("databaseBootstrapSql", "").trim();
        if (sqlText.isEmpty()) {
            return "connected:no-bootstrap-sql";
        }
        int executed = 0;
        for (String sql : sqlText.split(";")) {
            String statement = sql.trim();
            if (statement.isEmpty()) {
                continue;
            }
            DaisyBaseSqlResult result = daisyBaseConnector.execute(databaseResourceId, statement);
            if (!result.success() && !result.message().startsWith("Table already exists:")) {
                throw new IllegalStateException("DaisyBase bootstrap failed: " + result.message());
            }
            executed++;
        }
        return "bootstrapped:" + executed;
    }

    private static MinecraftRuntimeContainerSpec containerSpec(Map<String, String> attributes) {
        return new MinecraftRuntimeContainerSpec(
                require(attributes, "containerServiceName"),
                require(attributes, "containerImage"),
                require(attributes, "containerCommand"),
                parseMap(require(attributes, "containerEnvironment")),
                parseMap(require(attributes, "containerPortBindings")),
                parseMap(require(attributes, "containerVolumes")),
                parseMap(require(attributes, "containerResourceLimits")),
                require(attributes, "containerRestartPolicy"),
                parseMap(require(attributes, "containerLabels")),
                require(attributes, "activeInstance"));
    }

    private static Map<String, String> startupFiles(Map<String, String> attributes) {
        Map<String, String> files = new LinkedHashMap<>();
        attributes.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("startupFile:"))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> files.put(entry.getKey().substring("startupFile:".length()), entry.getValue()));
        if (files.isEmpty()) {
            throw new IllegalArgumentException("startup files are required for runtime execution");
        }
        return files;
    }

    private static MinecraftRuntimeExecutionResult failure(Map<String, String> attributes, String message) {
        Map<String, String> failed = new LinkedHashMap<>(attributes);
        failed.put("nodeAgentDispatchState", "completed");
        failed.put("nodeAgentStatus", "failed");
        failed.put("nodeAgentStatusReason", message == null || message.isBlank() ? "runtime execution failed" : message);
        failed.put("containerApplyState", "node-agent-failed");
        failed.put("provisioningState", "Failed");
        failed.put("observedState", "NodeAgentFailed");
        failed.put("healthState", "unknown");
        return MinecraftRuntimeExecutionResult.failed(Map.copyOf(failed), failed.get("nodeAgentStatusReason"));
    }

    private static Map<String, String> parseMap(String value) {
        Map<String, String> parsed = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return parsed;
        }
        for (String entry : value.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator < 1) {
                throw new IllegalArgumentException("Invalid runtime map entry: " + entry);
            }
            parsed.put(entry.substring(0, separator), entry.substring(separator + 1));
        }
        return Map.copyOf(parsed);
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

    private static String executionId(String executionRequestId, Map<String, String> attributes) {
        return "minecraft-runtime-" + sha256(executionRequestId
                + "\n"
                + attributes.getOrDefault("nodeAgentIdempotencyKey", "")
                + "\n"
                + attributes.getOrDefault("nodeAgentReconcileDigest", "")).substring(0, 16);
    }

    private static String require(Map<String, String> attributes, String name) {
        return requireText(attributes.get(name), name);
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

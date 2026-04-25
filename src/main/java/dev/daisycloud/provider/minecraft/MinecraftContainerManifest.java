package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MinecraftContainerManifest(
        String manifestVersion,
        String serviceName,
        String image,
        String command,
        Map<String, String> environment,
        Map<String, String> portBindings,
        Map<String, String> volumes,
        Map<String, String> resourceLimits,
        String restartPolicy,
        MinecraftStartupFiles startupFiles,
        Map<String, String> healthChecks,
        Map<String, String> labels) {
    public MinecraftContainerManifest {
        manifestVersion = requireText(manifestVersion, "manifestVersion");
        serviceName = requireText(serviceName, "serviceName");
        image = requireText(image, "image");
        command = requireText(command, "command");
        environment = copyMap(environment, "environment");
        portBindings = copyMap(portBindings, "portBindings");
        volumes = copyMap(volumes, "volumes");
        resourceLimits = copyMap(resourceLimits, "resourceLimits");
        restartPolicy = requireText(restartPolicy, "restartPolicy");
        startupFiles = Objects.requireNonNull(startupFiles, "startupFiles must not be null");
        healthChecks = copyMap(healthChecks, "healthChecks");
        labels = copyMap(labels, "labels");
    }

    public static MinecraftContainerManifest fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String serverName = requirePlanned(planned, "serverName");
        String edition = requirePlanned(planned, "edition");
        String serverType = requirePlanned(planned, "serverType");
        String image = requirePlanned(planned, "serverImage");
        String port = requirePlanned(planned, "port");
        String volume = requirePlanned(planned, "dataVolume");
        String memoryMb = requirePlanned(planned, "memoryMb");
        String vcpu = requirePlanned(planned, "vcpu");
        String storageGb = requirePlanned(planned, "storageGb");

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("EULA", "TRUE");
        environment.put("DAISY_MINECRAFT_EDITION", edition);
        environment.put("DAISY_MINECRAFT_SERVER_TYPE", serverType);
        environment.put("DAISY_MINECRAFT_VERSION", requirePlanned(planned, "minecraftVersion"));
        environment.put("DAISY_MINECRAFT_MEMORY_MB", memoryMb);
        environment.put("DAISY_MINECRAFT_JAVA_VERSION", requirePlanned(planned, "javaVersion"));
        environment.put("DAISY_MINECRAFT_RESOURCE_ID", planned.getOrDefault("resourceId", ""));
        environment.put("DAISY_MINECRAFT_CONTENT_SOURCE", requirePlanned(planned, "modSource"));
        environment.put("DAISY_MINECRAFT_PANEL_ACCESS", requirePlanned(planned, "panelAccess"));
        environment.put("DAISY_MINECRAFT_RESTART_SCHEDULE", requirePlanned(planned, "restartSchedule"));
        environment.put("DAISY_MINECRAFT_DATABASE_MODE", requirePlanned(planned, "databaseMode"));
        environment.put("DAISY_MINECRAFT_DATABASE_RESOURCE_ID", planned.getOrDefault("databaseResourceId", ""));
        environment.put("DAISY_MINECRAFT_MARKETPLACE_MODE", requirePlanned(planned, "marketplaceMode"));
        environment.put("DAISY_MINECRAFT_MARKETPLACE_SOURCES", requirePlanned(planned, "marketplaceSources"));
        environment.put("DAISY_MINECRAFT_ACTIVE_INSTANCE", requirePlanned(planned, "activeInstance"));
        environment.put("DAISY_MINECRAFT_INSTANCE_MANAGER", requirePlanned(planned, "instanceManagerState"));
        environment.put("DAISY_MINECRAFT_BUNDLED_ADDONS", planned.getOrDefault("bundledAddonIds", ""));
        environment.put("DAISY_MINECRAFT_DAISY_COMPANION", requirePlanned(planned, "daisyCompanion"));

        Map<String, String> ports = new LinkedHashMap<>();
        ports.put(port + "/tcp", port);
        ports.put(port + "/udp", port);

        Map<String, String> volumes = new LinkedHashMap<>();
        volumes.put(volume, "/data");

        Map<String, String> resourceLimits = new LinkedHashMap<>();
        resourceLimits.put("memoryMb", memoryMb);
        resourceLimits.put("vcpu", vcpu);
        resourceLimits.put("storageGb", storageGb);
        resourceLimits.put("storageMount", "/data");

        Map<String, String> healthChecks = new LinkedHashMap<>();
        healthChecks.put("process", "server process is running");
        healthChecks.put("port", "game port " + port + " is reachable");
        healthChecks.put("console", "console accepted startup log stream");
        healthChecks.put("tps", "tick health telemetry is available after warmup");
        if (!"disabled".equals(requirePlanned(planned, "databaseMode"))) {
            healthChecks.put("database", "DaisyBase control-plane endpoint is reachable");
        }

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("dev.daisycloud.provider", MinecraftServerProviderCatalog.PROVIDER_ID);
        labels.put("dev.daisycloud.resourceType", MinecraftServerProviderCatalog.SERVER_RESOURCE_TYPE);
        labels.put("dev.daisycloud.serverName", serverName);
        labels.put("dev.daisycloud.region", requirePlanned(planned, "region"));
        labels.put("dev.daisycloud.backupPolicy", requirePlanned(planned, "backupPolicy"));
        labels.put("dev.daisycloud.adminPanelState", requirePlanned(planned, "adminPanelState"));
        labels.put("dev.daisycloud.networkExposure", requirePlanned(planned, "networkExposure"));
        labels.put("dev.daisycloud.databaseMode", requirePlanned(planned, "databaseMode"));
        labels.put("dev.daisycloud.marketplaceMode", requirePlanned(planned, "marketplaceMode"));
        labels.put("dev.daisycloud.activeInstance", requirePlanned(planned, "activeInstance"));
        labels.put("dev.daisycloud.bundledAddons", planned.getOrDefault("bundledAddonIds", ""));
        labels.put("dev.daisycloud.daisyCompanion", requirePlanned(planned, "daisyCompanion"));

        return new MinecraftContainerManifest(
                "daisyminecraft.container.v1",
                "mc-" + serverName,
                image,
                commandFor(edition, planned),
                environment,
                ports,
                volumes,
                resourceLimits,
                "unless-stopped;scheduled=" + requirePlanned(planned, "restartSchedule"),
                MinecraftStartupFiles.fromPlannedAttributes(planned),
                healthChecks,
                labels);
    }

    public Map<String, String> toProviderAttributes(String requestId) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("containerManifestVersion", manifestVersion);
        attributes.put("containerServiceName", serviceName);
        attributes.put("containerImage", image);
        attributes.put("containerCommand", command);
        attributes.put("containerEnvironment", joinMap(environment));
        attributes.put("containerPortBindings", joinMap(portBindings));
        attributes.put("containerVolumes", joinMap(volumes));
        attributes.put("containerResourceLimits", joinMap(resourceLimits));
        attributes.put("containerRestartPolicy", restartPolicy);
        attributes.put("containerHealthChecks", joinMap(healthChecks));
        attributes.put("containerLabels", joinMap(labels));
        attributes.put("startupFileNames", String.join(",", startupFiles.files().keySet()));
        for (Map.Entry<String, String> file : startupFiles.files().entrySet()) {
            attributes.put("startupFile:" + file.getKey(), file.getValue());
        }
        attributes.put("containerApplyRequestId", requestId);
        attributes.put("containerApplyState", "planned-for-node-agent");
        return Map.copyOf(attributes);
    }

    private static String commandFor(String edition, Map<String, String> planned) {
        if ("bedrock".equals(edition)) {
            return "./bedrock_server";
        }
        String jvmArgs = planned.getOrDefault("jvmArgs", "").trim();
        String memoryMb = requirePlanned(planned, "memoryMb");
        String prefix = jvmArgs.isBlank()
                ? "java -Xms" + memoryMb + "M -Xmx" + memoryMb + "M"
                : "java " + jvmArgs;
        return prefix + " -jar server.jar nogui";
    }

    private static Map<String, String> copyMap(Map<String, String> values, String name) {
        Map<String, String> copied = new LinkedHashMap<>(Objects.requireNonNull(values, name + " must not be null"));
        for (Map.Entry<String, String> entry : copied.entrySet()) {
            requireText(entry.getKey(), name + " key");
            Objects.requireNonNull(entry.getValue(), name + " value must not be null");
        }
        return Map.copyOf(copied);
    }

    private static String requirePlanned(Map<String, String> attributes, String name) {
        return requireText(attributes.get(name), name);
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
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
}

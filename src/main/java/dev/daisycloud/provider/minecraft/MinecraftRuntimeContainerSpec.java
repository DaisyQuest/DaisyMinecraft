package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MinecraftRuntimeContainerSpec(
        String serviceName,
        String image,
        String command,
        Map<String, String> environment,
        Map<String, String> portBindings,
        Map<String, String> volumes,
        Map<String, String> resourceLimits,
        String restartPolicy,
        Map<String, String> labels,
        String activeInstance) {
    public MinecraftRuntimeContainerSpec {
        serviceName = requireText(serviceName, "serviceName");
        image = requireText(image, "image");
        command = requireText(command, "command");
        environment = copyMap(environment, "environment");
        portBindings = copyMap(portBindings, "portBindings");
        volumes = copyMap(volumes, "volumes");
        resourceLimits = copyMap(resourceLimits, "resourceLimits");
        restartPolicy = requireText(restartPolicy, "restartPolicy");
        labels = copyMap(labels, "labels");
        activeInstance = requireText(activeInstance, "activeInstance");
    }

    private static Map<String, String> copyMap(Map<String, String> values, String name) {
        Map<String, String> copied = new LinkedHashMap<>(Objects.requireNonNull(values, name + " must not be null"));
        for (Map.Entry<String, String> entry : copied.entrySet()) {
            requireText(entry.getKey(), name + " key");
            Objects.requireNonNull(entry.getValue(), name + " value must not be null");
        }
        return Map.copyOf(copied);
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

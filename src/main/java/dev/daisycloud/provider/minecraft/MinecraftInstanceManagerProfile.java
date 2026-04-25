package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MinecraftInstanceManagerProfile(
        String schemaVersion,
        String state,
        String activeInstance,
        int maxInstances,
        List<String> instanceSlots,
        String switchPolicy,
        List<String> databaseTables,
        String evidence) {
    public MinecraftInstanceManagerProfile {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        state = requireText(state, "state");
        activeInstance = requireText(activeInstance, "activeInstance");
        if (maxInstances < 1) {
            throw new IllegalArgumentException("maxInstances must be at least 1");
        }
        instanceSlots = List.copyOf(Objects.requireNonNull(instanceSlots, "instanceSlots must not be null"));
        switchPolicy = requireText(switchPolicy, "switchPolicy");
        databaseTables = List.copyOf(Objects.requireNonNull(databaseTables, "databaseTables must not be null"));
        evidence = requireText(evidence, "evidence");
    }

    public static MinecraftInstanceManagerProfile fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String state = requirePlanned(planned, "instanceManager");
        String activeInstance = requirePlanned(planned, "activeInstance");
        int maxInstances = Integer.parseInt(requirePlanned(planned, "maxInstances"));
        String switchPolicy = requirePlanned(planned, "instanceSwitchPolicy");
        List<String> slots = "disabled".equals(state)
                ? List.of(activeInstance + ":active")
                : instanceSlots(activeInstance, maxInstances);
        return new MinecraftInstanceManagerProfile(
                "daisyminecraft.instances.v1",
                state,
                activeInstance,
                maxInstances,
                slots,
                switchPolicy,
                List.of("server_instances", "instance_snapshots", "runtime_health"),
                "state=" + state
                        + ";active=" + activeInstance
                        + ";maxInstances=" + maxInstances
                        + ";switchPolicy=" + switchPolicy);
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("instanceManagerSchema", schemaVersion);
        attributes.put("instanceManagerState", state);
        attributes.put("activeInstance", activeInstance);
        attributes.put("maxInstances", Integer.toString(maxInstances));
        attributes.put("instanceSlots", String.join(",", instanceSlots));
        attributes.put("instanceSwitchPolicy", switchPolicy);
        attributes.put("instanceDatabaseTables", String.join(",", databaseTables));
        attributes.put("instanceEvidence", evidence);
        return Map.copyOf(attributes);
    }

    private static List<String> instanceSlots(String activeInstance, int maxInstances) {
        List<String> defaults = List.of(
                activeInstance + ":active",
                "staging:available",
                "rollback:reserved",
                "upgrade-preview:available",
                "world-import:available");
        if (maxInstances <= defaults.size()) {
            return defaults.subList(0, maxInstances);
        }
        java.util.ArrayList<String> slots = new java.util.ArrayList<>(defaults);
        for (int index = defaults.size() + 1; index <= maxInstances; index++) {
            slots.add("saved-" + index + ":available");
        }
        return List.copyOf(slots);
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

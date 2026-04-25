package dev.daisycloud.provider.minecraft;

import java.util.Map;
import java.util.Objects;

public record MinecraftRuntimeExecutionResult(
        boolean success,
        Map<String, String> attributes,
        String message) {
    public MinecraftRuntimeExecutionResult {
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        message = Objects.requireNonNull(message, "message must not be null").trim();
    }

    public static MinecraftRuntimeExecutionResult success(Map<String, String> attributes, String message) {
        return new MinecraftRuntimeExecutionResult(true, attributes, message);
    }

    public static MinecraftRuntimeExecutionResult failed(Map<String, String> attributes, String message) {
        return new MinecraftRuntimeExecutionResult(false, attributes, message);
    }
}

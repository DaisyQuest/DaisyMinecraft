package dev.daisycloud.provider.spi;

import java.util.List;
import java.util.Objects;

/**
 * Provider-published lifecycle, schema, diagnostic, and release evidence declarations.
 */
public record ProviderLifecycleCapabilities(
        List<ProviderOperationKind> operationKinds,
        List<String> schemaFields,
        List<String> diagnosticSignals,
        List<String> releaseEvidence) {
    public ProviderLifecycleCapabilities {
        operationKinds = List.copyOf(Objects.requireNonNull(operationKinds, "operationKinds must not be null"));
        schemaFields = copyText(schemaFields, "schemaFields");
        diagnosticSignals = copyText(diagnosticSignals, "diagnosticSignals");
        releaseEvidence = copyText(releaseEvidence, "releaseEvidence");
        if (operationKinds.isEmpty()) {
            throw new IllegalArgumentException("operationKinds must not be empty");
        }
        if (schemaFields.isEmpty()) {
            throw new IllegalArgumentException("schemaFields must not be empty");
        }
        if (diagnosticSignals.isEmpty()) {
            throw new IllegalArgumentException("diagnosticSignals must not be empty");
        }
        if (releaseEvidence.isEmpty()) {
            throw new IllegalArgumentException("releaseEvidence must not be empty");
        }
    }

    public static ProviderLifecycleCapabilities standard() {
        return new ProviderLifecycleCapabilities(
                List.of(ProviderOperationKind.values()),
                List.of("resourceId", "desiredProperties", "observedState", "diagnostics"),
                List.of("validate", "plan", "apply", "observe", "backup", "restore", "diagnose"),
                List.of("registration", "validate-plan-conformance", "lifecycle-operation-conformance"));
    }

    private static List<String> copyText(List<String> values, String name) {
        List<String> copied = List.copyOf(Objects.requireNonNull(values, name + " must not be null"));
        for (String value : copied) {
            requireText(value, name + " item");
        }
        return copied;
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

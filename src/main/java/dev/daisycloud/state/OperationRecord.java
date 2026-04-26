package dev.daisycloud.state;

import java.util.Map;
import java.util.Objects;

/**
 * Persisted operation state stored by the repository.
 */
public record OperationRecord(
        String operationId,
        String resourceId,
        String operationKind,
        String status,
        Map<String, String> attributes) {

    public OperationRecord {
        operationId = requireText(operationId, "operationId");
        resourceId = requireText(resourceId, "resourceId");
        operationKind = requireText(operationKind, "operationKind");
        status = requireText(status, "status");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

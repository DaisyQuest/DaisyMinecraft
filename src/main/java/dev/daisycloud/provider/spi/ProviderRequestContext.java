package dev.daisycloud.provider.spi;

import java.util.Map;
import java.util.Objects;

/**
 * Carries request-scoped data into a provider operation.
 */
public record ProviderRequestContext(
        String requestId,
        ProviderOperationKind operationKind,
        String providerId,
        String resourceTypeId,
        String resourceId,
        Map<String, String> attributes) {

    public ProviderRequestContext {
        requestId = requireText(requestId, "requestId");
        operationKind = Objects.requireNonNull(operationKind, "operationKind must not be null");
        providerId = normalizeOptionalText(providerId, "providerId");
        resourceTypeId = normalizeOptionalText(resourceTypeId, "resourceTypeId");
        resourceId = normalizeOptionalText(resourceId, "resourceId");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static String normalizeOptionalText(String value, String name) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

package dev.daisycloud.provider.spi;

import java.util.Objects;

/**
 * Preserves provider failure classification when a provider hook rejects a request.
 */
public final class ProviderOperationException extends RuntimeException {
    private final String providerId;
    private final String resourceTypeId;
    private final ProviderOperationKind operationKind;
    private final ProviderErrorClassification classification;

    public ProviderOperationException(
            String providerId,
            String resourceTypeId,
            ProviderOperationKind operationKind,
            ProviderErrorClassification classification,
            String message) {
        super(buildMessage(providerId, resourceTypeId, operationKind, classification, message));
        this.providerId = requireText(providerId, "providerId");
        this.resourceTypeId = requireText(resourceTypeId, "resourceTypeId");
        this.operationKind = Objects.requireNonNull(operationKind, "operationKind must not be null");
        this.classification = Objects.requireNonNull(classification, "classification must not be null");
    }

    public String providerId() {
        return providerId;
    }

    public String resourceTypeId() {
        return resourceTypeId;
    }

    public ProviderOperationKind operationKind() {
        return operationKind;
    }

    public ProviderErrorClassification classification() {
        return classification;
    }

    private static String buildMessage(
            String providerId,
            String resourceTypeId,
            ProviderOperationKind operationKind,
            ProviderErrorClassification classification,
            String message) {
        return "Provider " + requireText(providerId, "providerId")
                + "/" + requireText(resourceTypeId, "resourceTypeId")
                + " " + Objects.requireNonNull(operationKind, "operationKind must not be null")
                + " failed with " + Objects.requireNonNull(classification, "classification must not be null")
                + ": " + requireText(message, "message");
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

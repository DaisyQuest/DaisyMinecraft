package dev.daisycloud.provider.spi;

import java.util.Objects;

/**
 * Declares a resource type exposed by a provider.
 */
public record ResourceTypeRegistration(
        String resourceTypeId,
        String displayName,
        ProviderLifecycleCapabilities capabilities) {
    public ResourceTypeRegistration(String resourceTypeId, String displayName) {
        this(resourceTypeId, displayName, ProviderLifecycleCapabilities.standard());
    }

    public ResourceTypeRegistration {
        resourceTypeId = requireText(resourceTypeId, "resourceTypeId");
        capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
        if (displayName != null) {
            displayName = displayName.trim();
            if (displayName.isEmpty()) {
                throw new IllegalArgumentException("displayName must not be blank");
            }
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

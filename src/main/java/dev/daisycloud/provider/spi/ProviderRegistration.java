package dev.daisycloud.provider.spi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Declares a provider and the resource types it exposes.
 */
public record ProviderRegistration(
        String providerId,
        String displayName,
        List<ResourceTypeRegistration> resourceTypes) {

    public ProviderRegistration {
        providerId = requireText(providerId, "providerId");
        if (displayName != null) {
            displayName = displayName.trim();
            if (displayName.isEmpty()) {
                throw new IllegalArgumentException("displayName must not be blank");
            }
        }
        resourceTypes = List.copyOf(Objects.requireNonNull(resourceTypes, "resourceTypes must not be null"));
        validateDistinctResourceTypes(resourceTypes);
    }

    private static void validateDistinctResourceTypes(List<ResourceTypeRegistration> resourceTypes) {
        Map<String, ResourceTypeRegistration> registrations = new LinkedHashMap<>();
        for (ResourceTypeRegistration registration : resourceTypes) {
            ResourceTypeRegistration value = Objects.requireNonNull(registration, "resourceTypes must not contain null");
            ResourceTypeRegistration existing = registrations.putIfAbsent(value.resourceTypeId(), value);
            if (existing != null) {
                throw new IllegalArgumentException("Duplicate resourceTypeId: " + value.resourceTypeId());
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

package dev.daisycloud.state;

import java.util.Map;
import java.util.Objects;

/**
 * Persisted resource state stored by the repository.
 */
public record ResourceRecord(
        String resourceId,
        String providerId,
        String resourceTypeId,
        Map<String, String> attributes) {

    public ResourceRecord {
        resourceId = requireText(resourceId, "resourceId");
        providerId = requireText(providerId, "providerId");
        resourceTypeId = requireText(resourceTypeId, "resourceTypeId");
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

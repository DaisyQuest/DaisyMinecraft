package dev.daisycloud.model;

import java.util.Objects;

/**
 * One resource type/name pair within a resource identifier.
 */
public record ResourceTypeName(String type, String name) {
    public ResourceTypeName {
        type = requireSegment(type, "type");
        name = requireSegment(name, "name");
    }

    private static String requireSegment(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        if (value.indexOf('/') >= 0) {
            throw new IllegalArgumentException(label + " must not contain '/'");
        }
        return value;
    }
}

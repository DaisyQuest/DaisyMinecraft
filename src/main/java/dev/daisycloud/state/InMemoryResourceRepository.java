package dev.daisycloud.state;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory {@link ResourceRepository} implementation for tests and bootstrapping.
 */
public final class InMemoryResourceRepository implements ResourceRepository {
    private final ConcurrentMap<String, ResourceRecord> resources = new ConcurrentHashMap<>();

    @Override
    public ResourceRecord create(ResourceRecord resource) {
        ResourceRecord value = Objects.requireNonNull(resource, "resource must not be null");
        ResourceRecord existing = resources.putIfAbsent(value.resourceId(), value);
        if (existing != null) {
            throw new IllegalStateException("Resource already exists: " + value.resourceId());
        }
        return value;
    }

    @Override
    public Optional<ResourceRecord> get(String resourceId) {
        return Optional.ofNullable(resources.get(requireText(resourceId, "resourceId")));
    }

    @Override
    public List<ResourceRecord> list() {
        return resources.values().stream()
                .sorted(Comparator.comparing(ResourceRecord::resourceId))
                .toList();
    }

    @Override
    public ResourceRecord update(ResourceRecord resource) {
        ResourceRecord value = Objects.requireNonNull(resource, "resource must not be null");
        ResourceRecord existing = resources.replace(value.resourceId(), value);
        if (existing == null) {
            throw new IllegalStateException("Resource does not exist: " + value.resourceId());
        }
        return value;
    }

    @Override
    public Optional<ResourceRecord> delete(String resourceId) {
        return Optional.ofNullable(resources.remove(requireText(resourceId, "resourceId")));
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

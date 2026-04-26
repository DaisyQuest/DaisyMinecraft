package dev.daisycloud.state;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for resource state.
 */
public interface ResourceRepository {
    ResourceRecord create(ResourceRecord resource);

    Optional<ResourceRecord> get(String resourceId);

    List<ResourceRecord> list();

    ResourceRecord update(ResourceRecord resource);

    Optional<ResourceRecord> delete(String resourceId);
}

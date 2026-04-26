package dev.daisycloud.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical representation of a DaisyCloud resource identifier.
 */
public final class ResourceId {
    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final String RESOURCE_GROUPS = "resourceGroups";
    private static final String PROVIDERS = "providers";
    private static final String MANAGEMENT_NAMESPACE = "DaisyCloud.Management";
    private static final String MANAGEMENT_GROUPS = "managementGroups";

    private final UUID subscriptionId;
    private final String resourceGroup;
    private final String providerNamespace;
    private final List<ResourceTypeName> resourceTypeNames;
    private final boolean managementGroup;
    private final String managementGroupName;
    private final String canonical;

    private ResourceId(
            UUID subscriptionId,
            String resourceGroup,
            String providerNamespace,
            List<ResourceTypeName> resourceTypeNames,
            boolean managementGroup,
            String managementGroupName,
            String canonical) {
        this.subscriptionId = subscriptionId;
        this.resourceGroup = resourceGroup;
        this.providerNamespace = providerNamespace;
        this.resourceTypeNames = List.copyOf(resourceTypeNames);
        this.managementGroup = managementGroup;
        this.managementGroupName = managementGroupName;
        this.canonical = canonical;
    }

    public static ResourceId parse(String value) {
        Objects.requireNonNull(value, "resourceId must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("resourceId must not be empty");
        }
        if (!value.startsWith("/")) {
            throw new IllegalArgumentException("resourceId must start with '/'");
        }
        if (value.length() == 1) {
            throw new IllegalArgumentException("resourceId must not be just '/'");
        }

        String[] segments = value.substring(1).split("/", -1);
        for (int index = 0; index < segments.length; index++) {
            if (segments[index].isEmpty()) {
                throw new IllegalArgumentException("resourceId contains an empty path segment at position " + (index + 1));
            }
        }

        if (segments.length >= 4 && SUBSCRIPTIONS.equals(segments[0])) {
            return parseSubscriptionId(value, segments);
        }
        if (segments.length == 4
                && PROVIDERS.equals(segments[0])
                && MANAGEMENT_NAMESPACE.equals(segments[1])
                && MANAGEMENT_GROUPS.equals(segments[2])) {
            return parseManagementGroupId(value, segments[3]);
        }

        throw new IllegalArgumentException("Unrecognized resourceId shape: " + value);
    }

    public static ResourceId managementGroup(String name) {
        String validatedName = validateSegment(name, "managementGroupName");
        String canonical = "/" + PROVIDERS + "/" + MANAGEMENT_NAMESPACE + "/" + MANAGEMENT_GROUPS + "/" + validatedName;
        return new ResourceId(null, null, null, List.of(), true, validatedName, canonical);
    }

    public UUID subscriptionId() {
        return subscriptionId;
    }

    public String resourceGroup() {
        return resourceGroup;
    }

    public String providerNamespace() {
        return providerNamespace;
    }

    public List<ResourceTypeName> resourceTypeNames() {
        return resourceTypeNames;
    }

    public boolean isManagementGroup() {
        return managementGroup;
    }

    public String managementGroupName() {
        return managementGroupName;
    }

    @Override
    public String toString() {
        return canonical;
    }

    @Override
    public int hashCode() {
        return canonical.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ResourceId that && canonical.equals(that.canonical);
    }

    private static ResourceId parseSubscriptionId(String original, String[] segments) {
        if (segments.length < 8) {
            throw new IllegalArgumentException(
                    "Subscription resourceId must have /subscriptions/{uuid}/resourceGroups/{rg}/providers/{namespace}/{type}/{name}");
        }
        if (!RESOURCE_GROUPS.equals(segments[2])) {
            throw new IllegalArgumentException("Expected 'resourceGroups' at segment 3");
        }
        if (!PROVIDERS.equals(segments[4])) {
            throw new IllegalArgumentException("Expected 'providers' at segment 5");
        }

        UUID subscriptionId;
        try {
            subscriptionId = UUID.fromString(segments[1]);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid subscription UUID: " + segments[1], ex);
        }

        String resourceGroup = validateSegment(segments[3], "resourceGroup");
        String providerNamespace = validateSegment(segments[5], "providerNamespace");
        List<ResourceTypeName> resourceTypeNames = parseTypeNamePairs(segments, 6);
        String canonical = buildCanonical(subscriptionId, resourceGroup, providerNamespace, resourceTypeNames);
        return new ResourceId(subscriptionId, resourceGroup, providerNamespace, resourceTypeNames, false, null, canonical);
    }

    private static ResourceId parseManagementGroupId(String original, String name) {
        String validatedName = validateSegment(name, "managementGroupName");
        String canonical = "/" + PROVIDERS + "/" + MANAGEMENT_NAMESPACE + "/" + MANAGEMENT_GROUPS + "/" + validatedName;
        return new ResourceId(null, null, null, List.of(), true, validatedName, canonical);
    }

    private static List<ResourceTypeName> parseTypeNamePairs(String[] segments, int startIndex) {
        int remaining = segments.length - startIndex;
        if (remaining < 2) {
            throw new IllegalArgumentException("ResourceId is missing the first type/name pair");
        }
        if ((remaining & 1) != 0) {
            throw new IllegalArgumentException("ResourceId has an incomplete type/name pair");
        }
        List<ResourceTypeName> pairs = new ArrayList<>(remaining / 2);
        for (int index = startIndex; index < segments.length; index += 2) {
            String type = validateSegment(segments[index], "resourceType");
            String name = validateSegment(segments[index + 1], "resourceName");
            pairs.add(new ResourceTypeName(type, name));
        }
        return pairs;
    }

    private static String buildCanonical(
            UUID subscriptionId,
            String resourceGroup,
            String providerNamespace,
            List<ResourceTypeName> resourceTypeNames) {
        StringBuilder builder = new StringBuilder()
                .append('/').append(SUBSCRIPTIONS).append('/').append(subscriptionId)
                .append('/').append(RESOURCE_GROUPS).append('/').append(resourceGroup)
                .append('/').append(PROVIDERS).append('/').append(providerNamespace);
        for (ResourceTypeName pair : resourceTypeNames) {
            builder.append('/').append(pair.type()).append('/').append(pair.name());
        }
        return builder.toString();
    }

    private static String validateSegment(String value, String label) {
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

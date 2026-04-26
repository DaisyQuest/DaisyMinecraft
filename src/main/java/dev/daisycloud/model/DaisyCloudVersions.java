package dev.daisycloud.model;

import java.util.List;

/**
 * Shared product and public API version metadata.
 */
public final class DaisyCloudVersions {
    public static final String PRODUCT_VERSION = "0.1.0-SNAPSHOT";
    public static final String CURRENT_API_VERSION = "2026-04-22-preview";
    public static final List<String> SUPPORTED_API_VERSIONS = List.of(CURRENT_API_VERSION);
    public static final int METADATA_SCHEMA_VERSION = 1;

    private DaisyCloudVersions() {
    }
}

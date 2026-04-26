package dev.daisycloud.provider.spi;

/**
 * High-level provider error buckets for request and response handling.
 */
public enum ProviderErrorClassification {
    NONE,
    VALIDATION,
    CONFLICT,
    NOT_FOUND,
    UNAUTHORIZED,
    TRANSIENT,
    INTERNAL
}

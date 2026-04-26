package dev.daisycloud.provider.spi;

import java.util.Map;
import java.util.Objects;

/**
 * Standard provider result wrapper.
 */
public record ProviderResponse<T>(
        boolean success,
        ProviderErrorClassification errorClassification,
        String message,
        T value,
        Map<String, String> attributes) {

    public ProviderResponse {
        errorClassification = Objects.requireNonNull(errorClassification, "errorClassification must not be null");
        message = normalizeOptionalText(message, "message");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        if (success && errorClassification != ProviderErrorClassification.NONE) {
            throw new IllegalArgumentException("Successful responses must use ProviderErrorClassification.NONE");
        }
        if (!success && errorClassification == ProviderErrorClassification.NONE) {
            throw new IllegalArgumentException("Failed responses must classify the error");
        }
    }

    public static <T> ProviderResponse<T> success(T value) {
        return new ProviderResponse<>(true, ProviderErrorClassification.NONE, null, value, Map.of());
    }

    public static <T> ProviderResponse<T> failure(
            ProviderErrorClassification errorClassification,
            String message) {
        return new ProviderResponse<>(false, errorClassification, message, null, Map.of());
    }

    private static String normalizeOptionalText(String value, String name) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

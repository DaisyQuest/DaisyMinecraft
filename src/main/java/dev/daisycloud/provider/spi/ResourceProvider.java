package dev.daisycloud.provider.spi;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime provider behavior for validating and planning desired resource state.
 */
public interface ResourceProvider {
    ProviderRegistration registration();

    default ProviderResponse<Map<String, String>> validate(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> plan(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> apply(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> observe(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> delete(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> importState(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> exportState(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> backup(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> restore(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }

    default ProviderResponse<Map<String, String>> diagnose(ProviderRequestContext context) {
        ProviderRequestContext value = Objects.requireNonNull(context, "context must not be null");
        return ProviderResponse.success(value.attributes());
    }
}

package dev.daisycloud.provider.daisybase;

import dev.daisycloud.provider.spi.ProviderErrorClassification;
import dev.daisycloud.provider.spi.ProviderRegistration;
import dev.daisycloud.provider.spi.ProviderRequestContext;
import dev.daisycloud.provider.spi.ProviderResponse;
import dev.daisycloud.provider.spi.ResourceProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class DaisyBaseProvider implements ResourceProvider {
    private static final Pattern DATABASE_NAME = Pattern.compile("[a-z][a-z0-9-]{0,62}");

    @Override
    public ProviderRegistration registration() {
        return DaisyBaseProviderCatalog.registration();
    }

    @Override
    public ProviderResponse<Map<String, String>> validate(ProviderRequestContext context) {
        Map<String, String> attributes = context.attributes();
        String databaseName = attributes.get("databaseName");
        if (databaseName == null || databaseName.isBlank()) {
            return validationFailure("databaseName is required");
        }
        if (!DATABASE_NAME.matcher(databaseName).matches()) {
            return validationFailure("databaseName must start with a lowercase letter and contain lowercase letters, digits, or hyphens");
        }
        String endpoint = attributes.get("endpoint");
        if (endpoint != null && !endpoint.startsWith("jdbc:daisybase://")) {
            return validationFailure("endpoint must use jdbc:daisybase://");
        }
        String writeEnabled = attributes.get("writeEnabled");
        if (writeEnabled != null
                && !"true".equalsIgnoreCase(writeEnabled)
                && !"false".equalsIgnoreCase(writeEnabled)) {
            return validationFailure("writeEnabled must be true or false");
        }
        return ProviderResponse.success(attributes);
    }

    @Override
    public ProviderResponse<Map<String, String>> plan(ProviderRequestContext context) {
        ProviderResponse<Map<String, String>> validation = validate(context);
        if (!validation.success()) {
            return validation;
        }
        Map<String, String> planned = new LinkedHashMap<>(context.attributes());
        String databaseName = planned.get("databaseName");
        planned.putIfAbsent("endpoint", "jdbc:daisybase://local/" + databaseName);
        planned.putIfAbsent("writeEnabled", "true");
        planned.putIfAbsent("sku", "developer");
        return ProviderResponse.success(planned);
    }

    private static ProviderResponse<Map<String, String>> validationFailure(String message) {
        return ProviderResponse.failure(ProviderErrorClassification.VALIDATION, message);
    }
}

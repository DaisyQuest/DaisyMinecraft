package dev.daisycloud.provider.daisybase;

import dev.daisycloud.provider.spi.ProviderRegistration;
import dev.daisycloud.provider.spi.ResourceTypeRegistration;

import java.util.List;

public final class DaisyBaseProviderCatalog {
    public static final String PROVIDER_ID = "DaisyCloud.DaisyBase";
    public static final String DATABASE_RESOURCE_TYPE = "databases";

    private DaisyBaseProviderCatalog() {
    }

    public static ProviderRegistration registration() {
        return new ProviderRegistration(
                PROVIDER_ID,
                "DaisyBase",
                List.of(new ResourceTypeRegistration(DATABASE_RESOURCE_TYPE, "Databases")));
    }
}

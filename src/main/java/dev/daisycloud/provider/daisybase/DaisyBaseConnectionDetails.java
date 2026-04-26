package dev.daisycloud.provider.daisybase;

public record DaisyBaseConnectionDetails(
        String databaseResourceId,
        String endpoint,
        String databaseName,
        boolean writeEnabled) {

    public DaisyBaseConnectionDetails {
        databaseResourceId = Text.require(databaseResourceId, "databaseResourceId");
        endpoint = Text.require(endpoint, "endpoint");
        databaseName = Text.require(databaseName, "databaseName");
    }
}

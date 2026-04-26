package dev.daisycloud.model;

/**
 * Common control-plane provisioning states.
 */
public enum ProvisioningState {
    UNKNOWN,
    CREATING,
    UPDATING,
    SUCCEEDED,
    FAILED,
    DELETING
}

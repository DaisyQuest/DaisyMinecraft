package dev.daisycloud.provider.spi;

/**
 * The operation categories a provider can handle.
 */
public enum ProviderOperationKind {
    VALIDATE,
    PLAN,
    APPLY,
    OBSERVE,
    DELETE,
    IMPORT,
    EXPORT,
    BACKUP,
    RESTORE,
    DIAGNOSE
}

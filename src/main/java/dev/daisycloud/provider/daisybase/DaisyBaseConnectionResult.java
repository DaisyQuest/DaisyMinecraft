package dev.daisycloud.provider.daisybase;

import java.util.Objects;

public record DaisyBaseConnectionResult(
        boolean connected,
        String message,
        DaisyBaseConnectionDetails details) {

    public DaisyBaseConnectionResult {
        message = Text.require(message, "message");
        if (connected) {
            details = Objects.requireNonNull(details, "details must not be null when connected");
        } else if (details != null) {
            throw new IllegalArgumentException("details must be null when not connected");
        }
    }

    public static DaisyBaseConnectionResult connected(DaisyBaseConnectionDetails details) {
        return new DaisyBaseConnectionResult(true, "connected", details);
    }

    public static DaisyBaseConnectionResult failed(String message) {
        return new DaisyBaseConnectionResult(false, message, null);
    }
}

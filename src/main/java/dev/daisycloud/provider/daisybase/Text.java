package dev.daisycloud.provider.daisybase;

import java.util.Objects;

final class Text {
    private Text() {
    }

    static String require(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}

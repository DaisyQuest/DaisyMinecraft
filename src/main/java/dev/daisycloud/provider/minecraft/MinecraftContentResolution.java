package dev.daisycloud.provider.minecraft;

import java.util.List;
import java.util.Objects;

public record MinecraftContentResolution(
        List<MinecraftContentLockItem> items,
        List<String> warnings,
        String resolverMode) {
    public MinecraftContentResolution {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        resolverMode = requireText(resolverMode, "resolverMode");
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}

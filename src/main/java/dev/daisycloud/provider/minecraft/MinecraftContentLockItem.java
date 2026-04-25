package dev.daisycloud.provider.minecraft;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record MinecraftContentLockItem(
        String kind,
        String source,
        String id,
        String loader,
        String minecraftVersion,
        String resolution,
        String sha256) {
    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");
    private static final Set<String> KINDS = Set.of("modpack", "mod", "plugin");
    private static final Set<String> RESOLUTIONS = Set.of("required", "optional", "bundled");

    public MinecraftContentLockItem {
        kind = requireOneOf(kind, KINDS, "kind");
        source = requireSafe(source, "source");
        id = requireSafe(id, "id");
        loader = requireSafe(loader, "loader");
        minecraftVersion = requireSafe(minecraftVersion, "minecraftVersion");
        resolution = requireOneOf(resolution, RESOLUTIONS, "resolution");
        sha256 = normalizeSha(sha256);
    }

    private static String requireOneOf(String value, Set<String> allowed, String name) {
        String text = requireText(value, name);
        if (!allowed.contains(text)) {
            throw new IllegalArgumentException(name + " must be one of " + String.join(", ", allowed));
        }
        return text;
    }

    private static String requireSafe(String value, String name) {
        String text = requireText(value, name);
        if (!SAFE_TOKEN.matcher(text).matches()) {
            throw new IllegalArgumentException(name + " must be a safe token");
        }
        return text;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }

    private static String normalizeSha(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (!text.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("sha256 must be a 64-character hex digest");
        }
        return text;
    }
}

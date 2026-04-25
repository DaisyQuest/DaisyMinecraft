package dev.daisycloud.minecraft.companion;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Objects;

public record DaisyCompanionSettings(
        String dogName,
        double healthMultiplier,
        double scaleMultiplier,
        long spawnDelayTicks,
        boolean onlyFirstJoin,
        DyeColor collarColor) {
    public static final String DEFAULT_DOG_NAME = "Daisy";
    public static final double DEFAULT_HEALTH_MULTIPLIER = 100.0D;
    public static final double DEFAULT_SCALE_MULTIPLIER = 2.0D;
    public static final long DEFAULT_SPAWN_DELAY_TICKS = 20L;
    public static final boolean DEFAULT_ONLY_FIRST_JOIN = true;
    public static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.YELLOW;

    public DaisyCompanionSettings {
        dogName = requireText(dogName, "dogName");
        if (dogName.length() > 64) {
            throw new IllegalArgumentException("dogName must not exceed 64 characters");
        }
        healthMultiplier = bounded(healthMultiplier, "healthMultiplier", 1.0D, 1_000.0D);
        scaleMultiplier = bounded(scaleMultiplier, "scaleMultiplier", 0.1D, 16.0D);
        if (spawnDelayTicks < 0L || spawnDelayTicks > 1_200L) {
            throw new IllegalArgumentException("spawnDelayTicks must be between 0 and 1200");
        }
        collarColor = Objects.requireNonNull(collarColor, "collarColor must not be null");
    }

    public static DaisyCompanionSettings defaults() {
        return new DaisyCompanionSettings(
                DEFAULT_DOG_NAME,
                DEFAULT_HEALTH_MULTIPLIER,
                DEFAULT_SCALE_MULTIPLIER,
                DEFAULT_SPAWN_DELAY_TICKS,
                DEFAULT_ONLY_FIRST_JOIN,
                DEFAULT_COLLAR_COLOR);
    }

    public static DaisyCompanionSettings from(ConfigurationSection config) {
        Objects.requireNonNull(config, "config must not be null");
        return new DaisyCompanionSettings(
                config.getString("dog-name", DEFAULT_DOG_NAME),
                config.getDouble("health-multiplier", DEFAULT_HEALTH_MULTIPLIER),
                config.getDouble("scale-multiplier", DEFAULT_SCALE_MULTIPLIER),
                config.getLong("spawn-delay-ticks", DEFAULT_SPAWN_DELAY_TICKS),
                config.getBoolean("only-first-join", DEFAULT_ONLY_FIRST_JOIN),
                parseDyeColor(config.getString("collar-color", DEFAULT_COLLAR_COLOR.name())));
    }

    public String toPluginConfig() {
        return "dog-name: " + dogName + "\n"
                + "health-multiplier: " + healthMultiplier + "\n"
                + "scale-multiplier: " + scaleMultiplier + "\n"
                + "spawn-delay-ticks: " + spawnDelayTicks + "\n"
                + "only-first-join: " + onlyFirstJoin + "\n"
                + "collar-color: " + collarColor.name() + "\n";
    }

    private static DyeColor parseDyeColor(String value) {
        String normalized = requireText(value, "collarColor").trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return DyeColor.valueOf(normalized);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("collarColor must be a Bukkit DyeColor", error);
        }
    }

    private static double bounded(double value, String name, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}

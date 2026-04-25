package dev.daisycloud.minecraft.companion;

import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaisyCompanionSettingsTest {
    @Test
    void defaultsMatchDaisyMinecraftCompanionContract() {
        DaisyCompanionSettings settings = DaisyCompanionSettings.defaults();

        assertEquals("Daisy", settings.dogName());
        assertEquals(100.0D, settings.healthMultiplier());
        assertEquals(2.0D, settings.scaleMultiplier());
        assertEquals(20L, settings.spawnDelayTicks());
        assertTrue(settings.onlyFirstJoin());
        assertEquals(DyeColor.YELLOW, settings.collarColor());
        assertTrue(settings.toPluginConfig().contains("dog-name: Daisy"));
        assertTrue(settings.toPluginConfig().contains("health-multiplier: 100.0"));
        assertTrue(settings.toPluginConfig().contains("scale-multiplier: 2.0"));
    }

    @Test
    void rejectsUnsafeCompanionTuning() {
        assertThrows(IllegalArgumentException.class, () -> new DaisyCompanionSettings(
                "",
                100.0D,
                2.0D,
                20L,
                true,
                DyeColor.YELLOW));
        assertThrows(IllegalArgumentException.class, () -> new DaisyCompanionSettings(
                "Daisy",
                0.5D,
                2.0D,
                20L,
                true,
                DyeColor.YELLOW));
        assertThrows(IllegalArgumentException.class, () -> new DaisyCompanionSettings(
                "Daisy",
                100.0D,
                32.0D,
                20L,
                true,
                DyeColor.YELLOW));
        assertThrows(IllegalArgumentException.class, () -> new DaisyCompanionSettings(
                "Daisy",
                100.0D,
                2.0D,
                1_201L,
                true,
                DyeColor.YELLOW));
    }
}

package dev.daisycloud.minecraft.companion;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class DaisyCompanionPlugin extends JavaPlugin implements Listener {
    private NamespacedKey receivedKey;
    private NamespacedKey companionDogKey;
    private NamespacedKey intendedHitPointsKey;
    private DaisyCompanionSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        receivedKey = new NamespacedKey(this, "daisy_companion_received");
        companionDogKey = new NamespacedKey(this, "daisy_companion_dog");
        intendedHitPointsKey = new NamespacedKey(this, "daisy_companion_intended_hit_points");
        settings = DaisyCompanionSettings.from(getConfig());
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Daisy companion enabled: new players receive a loyal dog named " + settings.dogName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (settings.onlyFirstJoin() && player.hasPlayedBefore()) {
            return;
        }
        if (hasReceivedCompanion(player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        getServer().getScheduler().runTaskLater(this, () -> spawnCompanion(playerId), settings.spawnDelayTicks());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDaisyDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) {
            return;
        }
        if (!wolf.getPersistentDataContainer().has(companionDogKey, PersistentDataType.BYTE)) {
            return;
        }
        AttributeInstance health = wolf.getAttribute(Attribute.MAX_HEALTH);
        Double intendedHitPoints = wolf.getPersistentDataContainer().get(intendedHitPointsKey, PersistentDataType.DOUBLE);
        if (health == null || intendedHitPoints == null || intendedHitPoints <= health.getValue()) {
            return;
        }
        event.setDamage(event.getDamage() * (health.getValue() / intendedHitPoints));
    }

    private void spawnCompanion(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if (player == null || !player.isOnline() || hasReceivedCompanion(player)) {
            return;
        }

        player.getWorld().spawn(player.getLocation(), Wolf.class, wolf -> configureWolf(wolf, player));
        player.getPersistentDataContainer().set(receivedKey, PersistentDataType.BYTE, (byte) 1);
        getLogger().info("Spawned Daisy companion for " + player.getName());
    }

    private void configureWolf(Wolf wolf, Player owner) {
        wolf.setTamed(true);
        wolf.setOwner(owner);
        wolf.setCustomName(settings.dogName());
        wolf.setCustomNameVisible(true);
        wolf.setCollarColor(settings.collarColor());
        wolf.setPersistent(true);
        wolf.setRemoveWhenFarAway(false);
        wolf.setSitting(false);
        wolf.getPersistentDataContainer().set(companionDogKey, PersistentDataType.BYTE, (byte) 1);

        AttributeInstance health = wolf.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            double normalDogHitPoints = Math.max(1.0D, health.getBaseValue());
            double daisyHitPoints = normalDogHitPoints * settings.healthMultiplier();
            try {
                health.setBaseValue(daisyHitPoints);
            } catch (IllegalArgumentException error) {
                getLogger().warning("Server rejected Daisy's requested max health; preserving 100x effective durability.");
            }
            wolf.getPersistentDataContainer().set(intendedHitPointsKey, PersistentDataType.DOUBLE, daisyHitPoints);
            wolf.setHealth(Math.max(1.0D, health.getValue()));
        }

        AttributeInstance scale = wolf.getAttribute(Attribute.SCALE);
        if (scale != null) {
            double normalDogScale = Math.max(0.1D, scale.getBaseValue());
            scale.setBaseValue(normalDogScale * settings.scaleMultiplier());
        }
    }

    private boolean hasReceivedCompanion(Player player) {
        return player.getPersistentDataContainer().has(receivedKey, PersistentDataType.BYTE);
    }
}

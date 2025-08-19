package me.yourname.trimeffects;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TrimEffects extends JavaPlugin {

    private final Map<String, String> trimEffects = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadEffectsFromConfig();

        // Apply effects every 5 seconds
        Bukkit.getScheduler().runTaskTimer(this, this::applyEffects, 100L, 100L);

        getLogger().info("TrimEffects enabled successfully!");
    }

    private void loadEffectsFromConfig() {
        FileConfiguration config = getConfig();
        for (String trimName : config.getConfigurationSection("trims").getKeys(false)) {
            String effectName = config.getString("trims." + trimName);
            trimEffects.put(trimName.toLowerCase(), effectName.toUpperCase());
        }
    }

    private void applyEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<String> playerTrims = getPlayerTrims(player);

            for (String trim : playerTrims) {
                String effect = trimEffects.get(trim);
                if (effect != null) {
                    applyEffect(player, effect);
                }
            }
        }
    }

    private Set<String> getPlayerTrims(Player player) {
        Set<String> trims = new HashSet<>();

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null) continue;

            if (armor.hasItemMeta() && armor.getItemMeta() instanceof org.bukkit.inventory.meta.ArmorMeta armorMeta) {
                if (armorMeta.hasTrim()) {
                    ArmorTrim trim = armorMeta.getTrim();
                    TrimPattern pattern = trim.getPattern();

                    NamespacedKey key = pattern.getKey(); // ✅ Works in Paper 1.21+
                    trims.add(key.getKey().toLowerCase());
                }
            }
        }

        return trims;
    }

    private void applyEffect(Player player, String effectName) {
        switch (effectName) {
            case "SPEED":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0, true, false, true));
                break;
            case "STRENGTH":
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 0, true, false, true));
                break;
            case "HASTE":
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 120, 0, true, false, true));
                break;
            case "HEALTH_BOOST":
                AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (attr != null) attr.setBaseValue(24.0);
                break;
            case "NIGHT_VISION":
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false, true));
                break;
            default:
                // no effect
                break;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("TrimEffects disabled!");
    }
}

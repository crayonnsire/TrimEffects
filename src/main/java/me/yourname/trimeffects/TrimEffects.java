package me.yourname.trimeffects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TrimEffects extends JavaPlugin implements Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TrimEffects enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TrimEffects disabled!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyTrimEffects(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeTrimEffects(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(this, () -> applyTrimEffects(player), 1L);
        }
    }

    private void applyTrimEffects(Player player) {
        removeTrimEffects(player);

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || !armor.hasItemMeta()) continue;
            if (!(armor.getItemMeta() instanceof ArmorMeta armorMeta)) continue;
            if (!armorMeta.hasTrim()) continue;

            ArmorTrim trim = armorMeta.getTrim();
            TrimPattern pattern = trim.getPattern();

            // ✅ FIXED: works on 1.20.1
            String patternName = pattern.getKey().getKey();

            // Check config for effect
            if (config.contains("trims." + patternName)) {
                String effectName = config.getString("trims." + patternName + ".effect");
                int amplifier = config.getInt("trims." + patternName + ".amplifier", 0);

                PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, true, false, true));
                }
            }
        }
    }

    private void removeTrimEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
}

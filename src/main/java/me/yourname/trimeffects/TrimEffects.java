package me.yourname.trimeffects;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TrimEffects extends JavaPlugin {

    private int intervalTicks;
    private int effectDuration;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        // schedule repeating application
        Bukkit.getScheduler().runTaskTimer(this, this::applyAllPlayers, 40L, intervalTicks);
        getLogger().info("TrimEffects enabled. Interval=" + intervalTicks + " ticks, duration=" + effectDuration + " ticks.");
    }

    private void reloadLocalConfig() {
        this.intervalTicks = Math.max(10, getConfig().getInt("settings.tick_interval", 40));
        this.effectDuration = Math.max(40, getConfig().getInt("settings.effect_duration", 80));
    }

    private void applyAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyForPlayer(p);
        }
    }

    private void applyForPlayer(Player player) {
        Set<String> trims = getActiveTrimKeys(player);
        for (String trim : trims) {
            String path = "trims." + trim;
            if (!getConfig().getBoolean(path + ".enabled", true)) continue;

            String effectName = getConfig().getString(path + ".effect", "");
            int amplifier = Math.max(0, getConfig().getInt(path + ".amplifier", 0));

            PotionEffectType type = resolveEffect(effectName);
            if (type == null) continue;

            // Re-apply with short duration; does not wipe other plugins' effects.
            PotionEffect effect = new PotionEffect(type, effectDuration, amplifier, true, false, true);
            player.addPotionEffect(effect);
        }
    }

    private Set<String> getActiveTrimKeys(Player player) {
        Set<String> keys = new HashSet<>();
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || !piece.hasItemMeta()) continue;
            if (!(piece.getItemMeta() instanceof ArmorMeta meta)) continue;
            if (!meta.hasTrim()) continue;
            ArmorTrim trim = meta.getTrim();
            if (trim == null) continue;
            TrimPattern pattern = trim.getPattern();
            if (pattern == null) continue;
            NamespacedKey key = pattern.getKey();                 // Paper 1.21+
            if (key == null) continue;
            keys.add(key.getKey().toLowerCase(Locale.ROOT));      // e.g. "sentry"
        }
        return keys;
    }

    private PotionEffectType resolveEffect(String name) {
        if (name == null || name.isEmpty()) return null;
        String n = name.trim().toUpperCase(Locale.ROOT);

        // Friendly aliases
        if (n.equals("HASTE")) n = "FAST_DIGGING";
        if (n.equals("STRENGTH")) n = "INCREASE_DAMAGE";
        if (n.equals("RESISTANCE")) n = "DAMAGE_RESISTANCE";

        // Standard lookup
        PotionEffectType type = PotionEffectType.getByName(n);
        if (type != null) return type;

        // Namespaced (e.g., "minecraft:speed")
        if (n.contains(":")) {
            PotionEffectType byKey = PotionEffectType.getByKey(NamespacedKey.fromString(n));
            if (byKey != null) return byKey;
        }

        getLogger().warning("Unknown effect in config: " + name);
        return null;
    }
}

package me.yourname.trimeffects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TrimEffects extends JavaPlugin implements Listener {

    private BukkitTask task;
    private static final UUID RAISER_UUID = UUID.fromString("d5c2a4b5-4f8e-4b2f-a0a4-16b1a6d6b7a1");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        int interval = Math.max(10, getConfig().getInt("settings.tick_interval", 40));
        task = Bukkit.getScheduler().runTaskTimer(this, this::tickAllPlayers, 40L, interval);
        getLogger().info("TrimEffects enabled (interval=" + interval + "t).");
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeRaiserHealthModifier(p);
        }
    }

    private void tickAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyPersistentBuffs(p);
        }
    }

    private Set<String> getActiveTrimKeys(Player p) {
        Set<String> keys = new HashSet<>();
        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item == null) continue;
            if (!(item.getItemMeta() instanceof ArmorMeta meta)) continue;
            ArmorTrim trim = meta.getTrim();
            if (trim == null) continue;
            TrimPattern pat = trim.getPattern();
            NamespacedKey key = Registry.TRIM_PATTERN.getKey(pat);
            if (key != null && "minecraft".equals(key.getNamespace())) {
                keys.add(key.getKey().toLowerCase());
            }
        }
        return keys;
    }

    private void addEffect(Player p, PotionEffectType type, int amplifier, int ticks) {
        if (type == null || ticks <= 0) return;
        p.addPotionEffect(new PotionEffect(type, ticks, amplifier, true, false, true));
    }

    private boolean isInWater(Player p) {
        Material m = p.getEyeLocation().getBlock().getType();
        return m == Material.WATER || m == Material.BUBBLE_COLUMN;
    }

    private boolean isMelee(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE");
    }

    private boolean isWearingElytra(Player p) {
        ItemStack chest = p.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }

    private void applyPersistentBuffs(Player p) {
        Set<String> keys = getActiveTrimKeys(p);
        final int refresh = getConfig().getInt("settings.tick_interval", 40) + 10;

        // Sentry - Resistance
        if (keys.contains("sentry") && getConfig().getBoolean("trims.sentry.enabled", true)) {
            int lvl = getConfig().getInt("trims.sentry.resistance_level", 0);
            addEffect(p, PotionEffectType.DAMAGE_RESISTANCE, lvl, refresh);
        }

        // Dune - food/saturation bumps
        if (keys.contains("dune") && getConfig().getBoolean("trims.dune.enabled", true)) {
            int food = getConfig().getInt("trims.dune.food_bump", 1);
            double sat = getConfig().getDouble("trims.dune.saturation_bump", 0.5);
            if (p.getFoodLevel() < 20 && food > 0) p.setFoodLevel(Math.min(20, p.getFoodLevel() + food));
            if (sat > 0) p.setSaturation(Math.min(20f, (float)(p.getSaturation() + sat)));
        }

        // Coast - Dolphin's Grace in water
        if (keys.contains("coast") && getConfig().getBoolean("trims.coast.enabled", true) && isInWater(p)) {
            addEffect(p, PotionEffectType.DOLPHINS_GRACE, 0, refresh);
        }

        // Wild - Jump Boost
        if (keys.contains("wild") && getConfig().getBoolean("trims.wild.enabled", true)) {
            int lvl = getConfig().getInt("trims.wild.jump_level", 0);
            addEffect(p, PotionEffectType.JUMP, lvl, refresh);
        }

        // Ward - Night Vision under Y
        if (keys.contains("ward") && getConfig().getBoolean("trims.ward.enabled", true)) {
            int underY = getConfig().getInt("trims.ward.under_y", 40);
            if (p.getLocation().getY() < underY) {
                int dur = getConfig().getInt("trims.ward.night_vision_duration", refresh + 200);
                int lvl = getConfig().getInt("trims.ward.night_vision_level", 0);
                addEffect(p, PotionEffectType.NIGHT_VISION, lvl, dur);
            }
        }

        // Vex - Speed
        if (keys.contains("vex") && getConfig().getBoolean("trims.vex.enabled", true)) {
            int lvl = getConfig().getInt("trims.vex.speed_level", 0);
            addEffect(p, PotionEffectType.SPEED, lvl, refresh);
        }

        // Tide - Water Breathing
        if (keys.contains("tide") && getConfig().getBoolean("trims.tide.enabled", true)) {
            addEffect(p, PotionEffectType.WATER_BREATHING, 0, refresh);
        }

        // Snout - Strength if holding melee
        if (keys.contains("snout") && getConfig().getBoolean("trims.snout.enabled", true)) {
            boolean req = getConfig().getBoolean("trims.snout.require_melee", true);
            if (!req || isMelee(p.getInventory().getItemInMainHand())) {
                int lvl = getConfig().getInt("trims.snout.strength_level", 0);
                addEffect(p, PotionEffectType.INCREASE_DAMAGE, lvl, refresh);
            }
        }

        // Rib - Fire Resistance
        if (keys.contains("rib") && getConfig().getBoolean("trims.rib.enabled", true)) {
            addEffect(p, PotionEffectType.FIRE_RESISTANCE, 0, refresh);
        }

        // Spire - Slow Falling if wearing Elytra
        if (keys.contains("spire") && getConfig().getBoolean("trims.spire.enabled", true) && isWearingElytra(p)) {
            addEffect(p, PotionEffectType.SLOW_FALLING, 0, refresh);
        }

        // Wayfinder - Luck
        if (keys.contains("wayfinder") && getConfig().getBoolean("trims.wayfinder.enabled", true)) {
            int lvl = getConfig().getInt("trims.wayfinder.luck_level", 0);
            addEffect(p, PotionEffectType.LUCK, lvl, refresh);
        }

        // Raiser - bonus max health
        if (keys.contains("raiser") && getConfig().getBoolean("trims.raiser.enabled", true)) {
            double bonus = getConfig().getDouble("trims.raiser.max_health_bonus", 1.0);
            applyRaiserHealthModifier(p, bonus);
        } else {
            removeRaiserHealthModifier(p);
        }

        // Shaper - Haste
        if (keys.contains("shaper") && getConfig().getBoolean("trims.shaper.enabled", true)) {
            int lvl = getConfig().getInt("trims.shaper.haste_level", 0);
            addEffect(p, PotionEffectType.FAST_DIGGING, lvl, refresh);
        }
        // Host handled in target event
        // Flow handled in sprint event
        // Eye & Bolt handled in attack event
        // Silence handled in attack event
    }

    private void applyRaiserHealthModifier(Player p, double amount) {
        var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        // Remove old
        removeRaiserHealthModifier(p);
        AttributeModifier mod = new AttributeModifier(RAISER_UUID, "trimeffects_raiser", amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST);
        attr.addModifier(mod);
        // Ensure current health not above new max
        if (p.getHealth() > attr.getValue()) {
            p.setHealth(attr.getValue());
        }
    }

    private void removeRaiserHealthModifier(Player p) {
        var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        for (AttributeModifier m : new ArrayList<>(attr.getModifiers())) {
            if (m.getUniqueId().equals(RAISER_UUID)) {
                attr.removeModifier(m);
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        Set<String> keys = getActiveTrimKeys(p);
        if (!keys.contains("host")) return;
        if (!getConfig().getBoolean("trims.host.enabled", true)) return;
        double chance = getConfig().getDouble("trims.host.untargetable_chance", 0.10);
        if (Math.random() < chance) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent e) {
        if (!e.isSprinting()) return;
        Player p = e.getPlayer();
        Set<String> keys = getActiveTrimKeys(p);
        if (!keys.contains("flow")) return;
        if (!getConfig().getBoolean("trims.flow.enabled", true)) return;
        int lvl = getConfig().getInt("trims.flow.sprint_speed_level", 1);
        int dur = getConfig().getInt("trims.flow.sprint_duration", 60);
        addEffect(p, PotionEffectType.SPEED, lvl, dur);
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        Set<String> keys = getActiveTrimKeys(p);

        // Eye - apply Weakness to target
        if (keys.contains("eye") && getConfig().getBoolean("trims.eye.enabled", true)) {
            double chance = getConfig().getDouble("trims.eye.weakness_chance", 0.05);
            if (Math.random() < chance && e.getEntity() instanceof LivingEntity target) {
                int lvl = getConfig().getInt("trims.eye.weakness_level", 0);
                int dur = getConfig().getInt("trims.eye.weakness_duration", 100);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, lvl, true, true, true));
            }
        }

        // Bolt - lightning effect
        if (keys.contains("bolt") && getConfig().getBoolean("trims.bolt.enabled", true)) {
            double chance = getConfig().getDouble("trims.bolt.chance", 0.02);
            if (Math.random() < chance) {
                e.getEntity().getWorld().strikeLightningEffect(e.getEntity().getLocation());
                int fire = getConfig().getInt("trims.bolt.fire_ticks", 60);
                e.getEntity().setFireTicks(Math.max(e.getEntity().getFireTicks(), fire));
            }
        }

        // Silence - sneak damage bonus
        if (keys.contains("silence") && getConfig().getBoolean("trims.silence.enabled", true)) {
            if (p.isSneaking()) {
                double mult = getConfig().getDouble("trims.silence.sneak_damage_multiplier", 1.10);
                e.setDamage(e.getDamage() * mult);
            }
        }
    }
}

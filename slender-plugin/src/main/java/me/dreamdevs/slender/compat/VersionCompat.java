package me.dreamdevs.slender.compat;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Version compatibility helper for effects that differ across MC versions.
 */
public class VersionCompat {

    public static void applyDarkness(Player player, int durationTicks, int amplifier) {
        PotionEffectType effectType = PotionEffectType.getByName("DARKNESS");
        if (effectType == null) {
            effectType = PotionEffectType.BLINDNESS; // Fallback para versiones < 1.19
        }
        player.addPotionEffect(new PotionEffect(effectType, durationTicks, amplifier, false, false));
    }

    public static void removeDarkness(Player player) {
        PotionEffectType effectType = PotionEffectType.getByName("DARKNESS");
        if (effectType == null) {
            effectType = PotionEffectType.BLINDNESS;
        }
        player.removePotionEffect(effectType);
    }
    
    public static PotionEffectType getPotionType(String... names) {
        for (String name : names) {
            PotionEffectType type = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
            if (type != null) return type;
            type = PotionEffectType.getByName(name);
            if (type != null) return type;
        }
        return PotionEffectType.BLINDNESS; // Final fallback
    }
}

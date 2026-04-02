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
}

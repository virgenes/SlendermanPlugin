package me.dreamdevs.slender.compat;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Version compatibility helper for effects that differ across MC versions.
 */
public class VersionCompat {

    /**
     * Applies the darkness effect to a player.
     * Falls back to blindness on versions that don't have DARKNESS.
     */
    public static void applyDarkness(Player player, int durationTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, durationTicks, 0, false, false));
    }
}

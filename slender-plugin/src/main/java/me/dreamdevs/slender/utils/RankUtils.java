package me.dreamdevs.slender.utils;

import me.dreamdevs.slender.api.utils.ColourUtil;

public class RankUtils {

    /**
     * Get a rank name based on the player's level.
     * @param level The player's level.
     * @return The formatted rank name.
     */
    public static String getRank(int level) {
        if (level < 5) return ColourUtil.colorize("&7Novice");
        if (level < 15) return ColourUtil.colorize("&aSurvivor");
        if (level < 30) return ColourUtil.colorize("&bHunter");
        if (level < 50) return ColourUtil.colorize("&eExpert");
        if (level < 75) return ColourUtil.colorize("&6Master");
        if (level < 100) return ColourUtil.colorize("&cLegend");
        return ColourUtil.colorize("&4God");
    }
    
}

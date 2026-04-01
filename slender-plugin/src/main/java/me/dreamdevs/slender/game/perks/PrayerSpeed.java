package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@PerkInfo(name = "Prayer Speed", icon = Material.CLOCK, role = Role.SURVIVOR)
public class PrayerSpeed implements Perk {

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7Your devotion makes you faster",
                "&7at collecting what matters.",
                "",
                "&7Page collection is &e25% faster&7.",
                "&7Your hands move with purpose."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    /** Returns the page collection speed multiplier (0.75 = 25% faster) */
    public double getCollectionSpeedMultiplier() {
        return 0.75;
    }
}

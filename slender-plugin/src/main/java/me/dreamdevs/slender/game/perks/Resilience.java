package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@PerkInfo(name = "Resilience", icon = Material.SHIELD, role = Role.SURVIVOR)
public class Resilience implements Perk {

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7Your mind is stronger than most.",
                "",
                "&7Reduces sanity drain by &e15%&7.",
                "&7You can withstand the SlenderMan's",
                "&7presence for longer periods."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    /** Returns the sanity drain reduction multiplier (0.85 = 15% reduction) */
    public double getDrainMultiplier() {
        return 0.85;
    }
}

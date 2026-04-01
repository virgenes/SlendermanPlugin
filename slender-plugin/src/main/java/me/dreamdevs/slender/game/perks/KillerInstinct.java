package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@PerkInfo(name = "Killer's Instinct", icon = Material.IRON_SWORD, role = Role.SLENDER)
public class KillerInstinct implements Perk {

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7Every time you hit a survivor,",
                "&7their aura will be visible to all",
                "&7players for 5 seconds."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }
}

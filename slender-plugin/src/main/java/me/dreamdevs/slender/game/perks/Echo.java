package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@PerkInfo(name = "Echo", icon = Material.ECHO_SHARD, role = Role.SURVIVOR)
public class Echo implements Perk {

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7Your ears are trained to detect",
                "&7the slightest disturbance.",
                "",
                "&7Increases the range at which you can",
                "&7hear the SlenderMan's static by &e50%&7."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    /** Returns the static detection range multiplier (1.5 = 50% increase) */
    public double getDetectionMultiplier() {
        return 1.5;
    }
}

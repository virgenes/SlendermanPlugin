package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

@PerkInfo(name = "Spirit", icon = Material.SOUL_LANTERN, role = Role.SURVIVOR)
public class Spirit implements Perk {

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7Even in death, you help your team.",
                "",
                "&7When eliminated, remaining allies",
                "&7gain &eSpeed I &7and &a+20 sanity",
                "&7for 15 seconds."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    /** Apply the spirit bonus to all surviving teammates */
    public void applyDeathBonus(Player deadPlayer, List<Player> allies) {
        for (Player ally : allies) {
            if (ally != null && ally.isOnline()) {
                ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0));
                ally.sendTitle(ColourUtil.colorize("&a&lSpirit's Blessing"), ColourUtil.colorize("&7" + deadPlayer.getName() + "&7 watches over you"), 10, 40, 10);
                ally.playSound(ally.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
    }
}

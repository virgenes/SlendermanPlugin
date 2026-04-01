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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@PerkInfo(name = "Pages Belongings", icon = Material.PAPER, role = Role.SLENDER)
public class PagesBelongings implements Perk {

    private final HashMap<UUID, Integer> pageCounts = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7You are obsessed when",
                "&7someone steals your pages.",
                "",
                "&7For each collected page,",
                "&7you get &eSpeed Effect &7up to &eSpeed III&7."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    public void onPageCollected(Player slenderMan) {
        UUID uuid = slenderMan.getUniqueId();
        int count = pageCounts.getOrDefault(uuid, 0) + 1;
        pageCounts.put(uuid, count);

        int level = Math.min(2, (count - 1) / 2);
        slenderMan.removePotionEffect(PotionEffectType.SPEED);
        slenderMan.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level));
    }

    public void clear(Player player) {
        pageCounts.remove(player.getUniqueId());
    }
}

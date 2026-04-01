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

@PerkInfo(name = "From The Dark", icon = Material.ENDER_PEARL, role = Role.SLENDER)
public class FromTheDark implements Perk {

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7SlenderMan exactly knows",
                "&7when to attack survivors.",
                "",
                "&7After you hit a survivor,",
                "&7all other survivors up to 5 meters",
                "&7will get &eSlowness I &7for 10 seconds."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    public void applyEffect(Player slenderMan, Player hitSurvivor) {
        double radius = 5.0;
        hitSurvivor.getWorld().getNearbyEntities(hitSurvivor.getLocation(), radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> !p.equals(hitSurvivor) && !p.equals(slenderMan))
                .forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0)));
    }
}

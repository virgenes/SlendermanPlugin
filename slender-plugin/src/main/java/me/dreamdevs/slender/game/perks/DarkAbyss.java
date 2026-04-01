package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@PerkInfo(name = "Dark Abyss", icon = Material.BLACK_DYE, role = Role.SLENDER)
public class DarkAbyss implements Perk {

    private final HashMap<UUID, Boolean> active = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7After you kill any survivor,",
                "&7&cDark Abyss &7activates.",
                "",
                "&7If &cDark Abyss &7is activated,",
                "&7all other survivors cannot",
                "&7use their torches.",
                "",
                "&7This perk deactivates completely",
                "&7after you kill second survivor."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    public void activate(Player player) {
        active.put(player.getUniqueId(), true);
    }

    public boolean isActive(Player player) {
        return active.getOrDefault(player.getUniqueId(), false);
    }

    public void deactivate(Player player) {
        active.put(player.getUniqueId(), false);
    }

    public void clear(Player player) {
        active.remove(player.getUniqueId());
    }
}

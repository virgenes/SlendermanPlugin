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

@PerkInfo(name = "Endless Agony", icon = Material.NETHER_STAR, role = Role.SLENDER)
public class EndlessAgony implements Perk {

    private final HashMap<UUID, Integer> tokens = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7SlenderMan always predicts how to",
                "&7punish survivors for their failed missions.",
                "",
                "&7For every killed survivor you gain",
                "&7one token up to &e3 tokens&7.",
                "",
                "&7Every token grants you &c+0.5 damage&7."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    public void addToken(Player player) {
        UUID uuid = player.getUniqueId();
        tokens.put(uuid, Math.min(3, tokens.getOrDefault(uuid, 0) + 1));
    }

    public int getTokens(Player player) {
        return tokens.getOrDefault(player.getUniqueId(), 0);
    }

    public double getBonusDamage(Player player) {
        return getTokens(player) * 0.5;
    }

    public void clearTokens(Player player) {
        tokens.remove(player.getUniqueId());
    }
}

package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@PerkInfo(name = "Better Together", icon = Material.BRICKS, role = Role.SURVIVOR)
public class BetterTogether implements Perk {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7You are the supporter for your team.",
                "",
                "&7Life was never easy for you",
                "&7and you had to understand",
                "&7how to cooperate with other people.",
                "",
                "&7Every time you use your torch,",
                "&7all survivors within &e4 meters",
                "&7will see everything for &e2 seconds&7."
        ));
    }

    @Override
    public void usePerk(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_BETTER_TOGETHER_COOLDOWN.toLong() * 1000L;

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < cooldown) {
            long remaining = (cooldown - (now - cooldowns.get(uuid))) / 1000L;
            player.sendMessage(ColourUtil.colorize("&cPerk on cooldown! &7(" + remaining + "s)"));
            return;
        }

        cooldowns.put(uuid, now);
        double radius = Config.PERK_BETTER_TOGETHER_RADIUS.toDouble();

        player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> {
                    GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(p);
                    return gp != null && gp.isInArena();
                })
                .forEach(p -> {
                    p.removePotionEffect(PotionEffectType.BLINDNESS);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0));
                    p.sendMessage(ColourUtil.colorize("&b&lBetter Together &7activated by &e" + player.getName()));
                });
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_BETTER_TOGETHER_COOLDOWN.toLong() * 1000L;
        return cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < cooldown;
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}

package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
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

@PerkInfo(name = "RUNAWAY", icon = Material.TORCH, role = Role.SURVIVOR)
public class Runaway implements Perk {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7Every time you use your",
                "&7torch to light up, this perk activates.",
                "",
                "&7If &bRUNAWAY &7is activated, you get",
                "&eSpeed I &7for a short duration."
        ));
    }

    @Override
    public void usePerk(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_RUNAWAY_COOLDOWN.toLong() * 1000L;

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < cooldown) {
            long remaining = (cooldown - (now - cooldowns.get(uuid))) / 1000L;
            player.sendMessage(ColourUtil.colorize("&cPerk on cooldown! &7(" + remaining + "s)"));
            return;
        }

        cooldowns.put(uuid, now);
        int duration = Config.PERK_RUNAWAY_DURATION_TICKS.toInt();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
        player.sendMessage(ColourUtil.colorize("&b&lRUNAWAY &7activated! &eSpeed I"));
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_RUNAWAY_COOLDOWN.toLong() * 1000L;
        return cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < cooldown;
    }

    public long getCooldownRemaining(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return 0;
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_RUNAWAY_COOLDOWN.toLong() * 1000L;
        long elapsed = now - cooldowns.get(uuid);
        if (elapsed >= cooldown) return 0;
        return (cooldown - elapsed) / 1000L;
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}

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

@PerkInfo(name = "Archaeologist", icon = Material.BOOK, role = Role.SURVIVOR)
public class Archaeologist implements Perk {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7You are always interested in",
                "&7anything what you find.",
                "",
                "&7Anytime you pick up &6Page",
                "&7you get &eRegeneration I and Speed I",
                "&7for 3 seconds."
        ));
    }

    @Override
    public void usePerk(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_ARCHAEOLOGIST_COOLDOWN.toLong() * 1000L;

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < cooldown) {
            long remaining = (cooldown - (now - cooldowns.get(uuid))) / 1000L;
            player.sendMessage(ColourUtil.colorize("&cPerk on cooldown! &7(" + remaining + "s)"));
            return;
        }

        cooldowns.put(uuid, now);
        int duration = Config.PERK_ARCHAEOLOGIST_NIGHT_VISION_TICKS.toInt();
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration / 2, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration / 2, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0));
        player.sendMessage(ColourUtil.colorize("&b&lArchaeologist &7activated!"));
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Config.PERK_ARCHAEOLOGIST_COOLDOWN.toLong() * 1000L;
        return cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < cooldown;
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}

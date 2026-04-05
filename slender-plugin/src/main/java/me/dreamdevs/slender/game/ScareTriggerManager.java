package me.dreamdevs.slender.game;

import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;

public class ScareTriggerManager extends BukkitRunnable {

    private final Arena arena;

    public ScareTriggerManager(Arena arena) {
        this.arena = arena;
    }

    @Override
    public void run() {
        if (arena.getArenaState() != me.dreamdevs.slender.api.game.ArenaState.RUNNING) return;

        for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
            Player player = entry.getKey();
            if (entry.getValue() != Role.SURVIVOR) continue;

            Iterator<Location> it = arena.getTrapLocations().iterator();
            while (it.hasNext()) {
                Location trapLoc = it.next();
                if (player.getWorld().equals(trapLoc.getWorld()) && player.getLocation().distance(trapLoc) < 1.5) {
                    triggerScare(player);
                    it.remove(); // Trap used
                    break;
                }
            }
        }
    }

    private void triggerScare(Player player) {
        // Visual Jump Scare (Title + Flash)
        Title title = Title.title(
                ColourUtil.colorizeToComponent("&0&lSLENDERMAN"),
                ColourUtil.colorizeToComponent("&c&lHE IS BEHIND YOU"),
                Title.Times.times(Ticks.duration(0), Ticks.duration(10), Ticks.duration(5))
        );
        player.showTitle(title);

        // Sound Effects
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.1f);

        // Potion Effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));

        // Particles
        player.spawnParticle(Particle.LARGE_SMOKE, player.getEyeLocation(), 50, 0.5, 0.5, 0.5, 0.05);
    }
}

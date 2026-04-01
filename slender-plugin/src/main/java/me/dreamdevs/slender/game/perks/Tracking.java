package me.dreamdevs.slender.game.perks;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

@PerkInfo(name = "Tracking", icon = Material.COMPASS, role = Role.SURVIVOR)
public class Tracking implements Perk {

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    @Override
    public List<String> getLore() {
        return ColourUtil.colouredLore(Arrays.asList(
                "",
                "&7You have an instinct for finding",
                "&7what others cannot see.",
                "",
                "&7Every &e30 seconds&7, particles point",
                "&7towards the nearest page location."
        ));
    }

    @Override
    public void usePerk(Player player) {
    }

    public void startTracking(Player player, Arena arena) {
        stopTracking(player);

        BukkitTask task = SlenderMain.getInstance().getServer().getScheduler().runTaskTimer(
                SlenderMain.getInstance(), () -> {
                    if (!player.isOnline() || !player.getWorld().equals(arena.getSlenderManSpawnLocation().getWorld())) {
                        stopTracking(player);
                        return;
                    }

                    Location nearest = findNearestPage(player, arena);
                    if (nearest != null) {
                        Location playerLoc = player.getLocation();
                        org.bukkit.util.Vector dir = nearest.toVector().subtract(playerLoc.toVector()).normalize().multiply(3);
                        Location direction = playerLoc.clone().add(dir);
                        direction.setY(playerLoc.getY() + 1);

                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, direction, 10, 0.3, 0.3, 0.3, 0);
                        player.sendActionBar(ColourUtil.colorize("&a&lTracking &7- Page nearby!"));
                    }
                }, 0L, 600L);

        tasks.put(player.getUniqueId(), task);
    }

    public void stopTracking(Player player) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void stopAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    private Location findNearestPage(Player player, Arena arena) {
        return arena.getPagesLocations().stream()
                .min(Comparator.comparingDouble(loc -> loc.distance(player.getLocation())))
                .orElse(null);
    }
}

package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.perks.Echo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TerrorRadiusManager {

    private static final Sound[] DISTANT_SOUNDS = {
            Sound.AMBIENT_CAVE,
            Sound.BLOCK_NOTE_BLOCK_BASS,
            Sound.ENTITY_ENDERMAN_AMBIENT
    };

    private static final Sound[] CLOSE_SOUNDS = {
            Sound.ENTITY_ENDERMAN_SCREAM,
            Sound.ENTITY_WITHER_AMBIENT,
            Sound.ENTITY_BAT_DEATH,
            Sound.BLOCK_NOTE_BLOCK_BASS
    };

    private BukkitTask task;

    public void start(Arena arena) {
        if (task != null) task.cancel();

        task = Bukkit.getScheduler().runTaskTimer(SlenderMain.getInstance(), () -> {
            if (arena.getSlenderMan() == null) return;
            Player slender = arena.getSlenderMan();
            if (slender == null || !slender.isOnline()) return;

            Location slenderLoc = slender.getLocation();

            for (Map.Entry<Player, Role> entry : new HashMap<>(arena.getPlayers()).entrySet()) {
                Player player = entry.getKey();
                Role role = entry.getValue();

                if (role != Role.SURVIVOR) continue;
                if (!player.isOnline() || player.getLocation().getWorld() != slenderLoc.getWorld()) continue;

                double distance = player.getLocation().distance(slenderLoc);

                // Apply Echo perk: +50% detection range
                GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
                double rangeMultiplier = 1.0;
                if (gp != null) {
                    Perk perk = gp.getPerk(Role.SURVIVOR);
                    if (perk instanceof Echo) {
                        rangeMultiplier = ((Echo) perk).getDetectionMultiplier(); // 1.5
                    }
                }

                double closeRange = 3.0 * rangeMultiplier;
                double distantRange = 7.0 * rangeMultiplier;

                if (distance <= closeRange) {
                    playCloseSound(player);
                    if (Config.USE_DARKNESS_EFFECT.toBoolean()) {
                        GamePlayer gpObj = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
                        boolean flicker = gpObj == null || gpObj.getSetting(me.dreamdevs.slender.api.Setting.DARKNESS_FLICKER) == null || (boolean) gpObj.getSetting(me.dreamdevs.slender.api.Setting.DARKNESS_FLICKER);
                        
                        if (flicker) {
                            PotionEffect active = player.getPotionEffect(PotionEffectType.BLINDNESS);
                            if (active == null || active.getDuration() < 20) {
                                me.dreamdevs.slender.compat.VersionCompat.applyDarkness(player, 100, 0);
                            }
                        } else {
                            // Flicker OFF: Darkness and Blindness are disabled
                            if (distance <= 30) {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                            }
                            if (distance <= 15) {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
                            }
                        }
                    }
                } else if (distance <= distantRange) {
                    playDistantSound(player);
                }
            }
        }, 0L, 60L);
    }

    private void playDistantSound(Player player) {
        Sound sound = DISTANT_SOUNDS[new Random().nextInt(DISTANT_SOUNDS.length)];
        player.playSound(player.getLocation(), sound, 0.3f, 0.8f);
    }

    private void playCloseSound(Player player) {
        Sound sound = CLOSE_SOUNDS[new Random().nextInt(CLOSE_SOUNDS.length)];
        player.playSound(player.getLocation(), sound, 0.8f, 0.5f);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

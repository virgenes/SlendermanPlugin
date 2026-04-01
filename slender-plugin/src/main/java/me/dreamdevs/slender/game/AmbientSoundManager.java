package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Horror ambient sound system: plays terrifying sounds.
 * All Bukkit API calls are properly synced to main thread.
 */
public class AmbientSoundManager {

    private static final Sound[] HORROR_SOUNDS = {
            Sound.ENTITY_WITHER_SPAWN,
            Sound.ENTITY_WITHER_AMBIENT,
            Sound.ENTITY_WITHER_HURT,
            Sound.ENTITY_ENDERMAN_SCREAM,
            Sound.ENTITY_GHAST_SCREAM,
            Sound.ENTITY_GHAST_WARN,
            Sound.ENTITY_GHAST_HURT,
            Sound.ENTITY_BAT_DEATH,
            Sound.AMBIENT_CAVE,
            Sound.BLOCK_NOTE_BLOCK_BASS,
            Sound.BLOCK_NOTE_BLOCK_HAT,
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            Sound.BLOCK_NOTE_BLOCK_BELL,
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
            Sound.BLOCK_PORTAL_AMBIENT,
            Sound.BLOCK_PORTAL_TRAVEL
    };

    private static final Sound[] HEARTBEAT_SOUNDS = {
            Sound.BLOCK_NOTE_BLOCK_BASS,
            Sound.BLOCK_NOTE_BLOCK_HAT
    };

    private BukkitTask task;

    public void start(Arena arena) {
        if (task != null) task.cancel();

        task = Bukkit.getScheduler().runTaskTimer(SlenderMain.getInstance(), () -> {
            if (arena.getSlenderMan() == null) return;
            Player slender = arena.getSlenderMan();
            if (slender == null || !slender.isOnline()) return;
            Location slenderLoc = slender.getLocation();

            for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
                Player player = entry.getKey();
                Role role = entry.getValue();
                if (role != Role.SURVIVOR || !player.isOnline()) continue;

                Location pLoc = player.getLocation();
                if (pLoc.getWorld() != slenderLoc.getWorld()) continue;

                double dist = pLoc.distance(slenderLoc);
                int lightLevel = pLoc.getBlock().getLightLevel();
                SanityManager sanity = arena.getSanityManager();
                double san = sanity != null ? sanity.getSanity(player) : 100.0;

                // === DISTANCE-BASED TERROR SOUNDS ===
                if (dist <= 5.0) {
                    if (ThreadLocalRandom.current().nextInt(100) < 40) {
                        playHorrorSound(player, pLoc, 1.0f, 0.1f);
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 30) {
                        applyGlitchEffect(player);
                    }
                } else if (dist <= 10.0) {
                    if (ThreadLocalRandom.current().nextInt(100) < 25) {
                        playHorrorSound(player, pLoc, 0.7f, 0.2f);
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 15) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                    }
                } else if (dist <= 20.0) {
                    if (ThreadLocalRandom.current().nextInt(100) < 12) {
                        playHorrorSound(player, pLoc, 0.4f, 0.3f);
                    }
                } else if (dist <= 35.0) {
                    if (ThreadLocalRandom.current().nextInt(100) < 5) {
                        playHorrorSound(player, pLoc, 0.2f, 0.4f);
                    }
                }

                // === DARKNESS TERROR ===
                if (lightLevel == 0 && ThreadLocalRandom.current().nextInt(100) < 8) {
                    playHorrorSound(player, pLoc, 0.3f, 0.5f);
                    player.playSound(pLoc, Sound.AMBIENT_CAVE, 0.5f, 0.3f);
                }

                // === SANITY-BASED HORROR ===
                if (san <= 10) {
                    if (ThreadLocalRandom.current().nextInt(100) < 50) {
                        playHorrorSound(player, pLoc, 0.8f, 0.1f);
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 20) {
                        applyGlitchEffect(player);
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 40) {
                        playHeartbeat(player, pLoc);
                    }
                } else if (san <= 30) {
                    if (ThreadLocalRandom.current().nextInt(100) < 20) {
                        playHorrorSound(player, pLoc, 0.5f, 0.2f);
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 10) {
                        playHeartbeat(player, pLoc);
                    }
                } else if (san <= 50) {
                    if (ThreadLocalRandom.current().nextInt(100) < 8) {
                        player.playSound(pLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 0.8f);
                    }
                }

                // === LIGHTNING STRIKE EFFECTS ===
                if (dist <= 8.0 && ThreadLocalRandom.current().nextInt(100) < 3) {
                    player.getWorld().strikeLightningEffect(pLoc.clone().add(
                            ThreadLocalRandom.current().nextDouble(-5, 5), 0,
                            ThreadLocalRandom.current().nextDouble(-5, 5)));
                    player.playSound(pLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.5f);
                }
            }
        }, 0L, 60L);
    }

    private void playHorrorSound(Player player, Location loc, float volume, float pitch) {
        Sound sound = HORROR_SOUNDS[ThreadLocalRandom.current().nextInt(HORROR_SOUNDS.length)];
        player.playSound(loc, sound, volume, pitch);
    }

    private void playHeartbeat(Player player, Location loc) {
        Sound beat = HEARTBEAT_SOUNDS[ThreadLocalRandom.current().nextInt(HEARTBEAT_SOUNDS.length)];
        player.playSound(loc, beat, 0.6f, 0.1f);
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () ->
                player.playSound(loc, beat, 0.4f, 0.15f), 6L);
    }

    private void applyGlitchEffect(Player player) {
        // Flash blindness
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 0));
        // Nausea warp
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 30, 0));
        // Screen shake via camera (rapid teleport back)
        Location original = player.getLocation().clone();
        player.teleport(original.add(0.05, 0, 0));
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
            player.teleport(original.subtract(0.05, 0, 0));
        }, 2L);
        // Static particles
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.6, 0), 20, 0.5, 1, 0.5, 0.02);
        // Static sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 0.1f);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

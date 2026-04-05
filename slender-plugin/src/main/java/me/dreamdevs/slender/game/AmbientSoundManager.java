package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.Difficulty;
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

            // Phase 4: HARD Mode Slenderman Debuff
            if (arena.getCurrentDifficulty() == Difficulty.HARD) {
                // Slenderman will reveal his position occasionally with Glowing and slowed down slightly
                if (ThreadLocalRandom.current().nextInt(100) < 15) { // 15% chance every 3 seconds
                    slender.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0)); // visible through walls for 3s
                    slender.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&eThe darkness recedes slightly... Your aura is revealed!"));
                    slender.playSound(slenderLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                }
            }

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
                        applyGlitchEffect(player, arena, slender);
                    }
                } else if (dist <= 10.0) {
                    if (ThreadLocalRandom.current().nextInt(100) < 25) {
                        playHorrorSound(player, pLoc, 0.7f, 0.2f);
                    }
                    // === DARKNESS TERROR ===
                    if (ThreadLocalRandom.current().nextInt(100) < 3 && me.dreamdevs.slender.api.Config.USE_DARKNESS_EFFECT.toBoolean()) {
                        me.dreamdevs.slender.database.data.GamePlayer gpObj = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
                        boolean flicker = gpObj == null || gpObj.getSetting(me.dreamdevs.slender.api.Setting.DARKNESS_FLICKER) == null || (boolean) gpObj.getSetting(me.dreamdevs.slender.api.Setting.DARKNESS_FLICKER);
                        
                        if (flicker) {
                            org.bukkit.potion.PotionEffect active = player.getPotionEffect(PotionEffectType.DARKNESS != null ? PotionEffectType.DARKNESS : PotionEffectType.BLINDNESS);
                            if (active == null || active.getDuration() < 20) {
                                me.dreamdevs.slender.compat.VersionCompat.applyDarkness(player, 80, 0);
                            }
                        } else {
                            // Flicker OFF: Darkness and Blindness are disabled
                            org.bukkit.potion.PotionEffectType dType = PotionEffectType.DARKNESS;
                            if (dType != null) player.removePotionEffect(dType);
                            player.removePotionEffect(PotionEffectType.BLINDNESS);
                        }
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
                        applyGlitchEffect(player, arena, slender);
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
                } // End of lightning strike
                if (arena.getCurrentDifficulty() == Difficulty.INTERMEDIATE || arena.getCurrentDifficulty() == Difficulty.HARD) {
                    // Constant paranoia: 15% chance to hear footsteps nearby
                    if (ThreadLocalRandom.current().nextInt(100) < 15) {
                        Location soundLoc = pLoc.clone().add(ThreadLocalRandom.current().nextDouble(-3, 3), 0, ThreadLocalRandom.current().nextDouble(-3, 3));
                        player.playSound(soundLoc, Sound.BLOCK_WOOD_STEP, 0.8f, 0.5f);
                    }
                    // 10% chance to hear a fake heartbeat
                    if (ThreadLocalRandom.current().nextInt(100) < 10) {
                        playHeartbeat(player, pLoc);
                    }
                    // 5% chance of fake explosion hallucination
                    if (ThreadLocalRandom.current().nextInt(100) < 5) {
                        Location explosionLoc = pLoc.clone().add(ThreadLocalRandom.current().nextDouble(-5, 5), 0, ThreadLocalRandom.current().nextDouble(-5, 5));
                        player.playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                        player.spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 1);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 1));
                    }
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

    private void applyGlitchEffect(Player player, Arena arena, Player slender) {
        if (slender != null && player.getLocation().distance(slender.getLocation()) < 15) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        } else {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        if (slender != null && player.getLocation().distance(slender.getLocation()) < 8) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        }
        
        // Intermediate/Hard exclusive: Random Explosions
        if (arena.getCurrentDifficulty() != Difficulty.EASY && ThreadLocalRandom.current().nextInt(100) < 2) {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
        }
        
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

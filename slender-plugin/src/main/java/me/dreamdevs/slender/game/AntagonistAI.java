package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Antagonist AI: Controls SlenderMan behavior with aggression phases.
 * Phase 1 (0-33% pages): Observer - SlenderMan is passive, watches from distance.
 * Phase 2 (33-66% pages): Stalker - SlenderMan gets closer, interferes with players.
 * Phase 3 (66-100% pages): Active Hunter - SlenderMan actively hunts survivors.
 * Each phase grants the SlenderMan increasing advantages.
 */
public class AntagonistAI {

    public enum Phase {
        OBSERVER("&7Observer", 0.0, 0.33),
        STALKER("&eStalker", 0.33, 0.66),
        HUNTER("&cHunter", 0.66, 1.0);

        private final String displayName;
        private final double minPercent;
        private final double maxPercent;

        Phase(String displayName, double minPercent, double maxPercent) {
            this.displayName = displayName;
            this.minPercent = minPercent;
            this.maxPercent = maxPercent;
        }

        public String getDisplayName() { return displayName; }
        public double getMinPercent() { return minPercent; }
        public double getMaxPercent() { return maxPercent; }

        public static Phase fromProgress(double progress) {
            for (Phase p : values()) {
                if (progress >= p.minPercent && progress < p.maxPercent) return p;
            }
            return HUNTER;
        }
    }

    private final Arena arena;
    private Phase currentPhase = Phase.OBSERVER;
    private BukkitTask task;
    private final Map<UUID, Long> staticInterference = new HashMap<>();
    private final Set<UUID> noiseAlerts = new HashSet<>();

    // Phase bonuses
    private double speedBonus = 0.0;
    private double damageBonus = 0.0;
    private int detectionRange = 10;

    public AntagonistAI(Arena arena) {
        this.arena = arena;
    }

    public void start() {
        if (task != null) task.cancel();

        task = Bukkit.getScheduler().runTaskTimer(SlenderMain.getInstance(), () -> {
            if (arena.getSlenderMan() == null || !arena.getSlenderMan().isOnline()) return;
            Player slender = arena.getSlenderMan();

            // Calculate progress based on pages collected
            int pagesToWin = Config.PAGES_TO_WIN.toInt();
            double progress = Math.min(1.0, (double) arena.getCollectedPages() / pagesToWin);

            // Update phase
            Phase newPhase = Phase.fromProgress(progress);
            if (newPhase != currentPhase) {
                currentPhase = newPhase;
                updatePhaseBonuses();
                arena.sendMessage(ChatColor.GOLD + "[SlenderMan] Phase changed to " + currentPhase.getDisplayName());
            }

            // Apply phase-specific behaviors
            applyPhaseEffects(slender);

            // Static interference for nearby players
            applyStaticInterference(slender);

            // Show noise alerts to SlenderMan
            showNoiseAlerts(slender);

            // Check for jumpscare conditions
            checkJumpscareConditions(slender);

        }, 0L, 20L);
    }

    private void updatePhaseBonuses() {
        switch (currentPhase) {
            case OBSERVER:
                speedBonus = 0.0;
                damageBonus = 0.0;
                detectionRange = 10;
                break;
            case STALKER:
                speedBonus = 0.1; // +10% speed
                damageBonus = 0.5; // +0.5 damage
                detectionRange = 15;
                break;
            case HUNTER:
                speedBonus = 0.25; // +25% speed
                damageBonus = 1.0; // +1.0 damage
                detectionRange = 20;
                break;
        }
    }

    private void applyPhaseEffects(Player slender) {
        // Apply speed bonus based on phase
        int speedLevel = (int) Math.round(speedBonus * 10);
        if (speedLevel > 0) {
            slender.removePotionEffect(PotionEffectType.SPEED);
            slender.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, speedLevel - 1, true, false));
        }

        // Phase 2+: Teleport closer to random survivor occasionally
        if (currentPhase == Phase.STALKER || currentPhase == Phase.HUNTER) {
            if (ThreadLocalRandom.current().nextInt(100) < (currentPhase == Phase.HUNTER ? 15 : 5)) {
                teleportToNearestSurvivor(slender);
            }
        }
    }

    private void applyStaticInterference(Player slender) {
        Location slenderLoc = slender.getLocation();
        double interferenceRange = currentPhase == Phase.HUNTER ? 12.0 : (currentPhase == Phase.STALKER ? 8.0 : 5.0);

        for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
            Player player = entry.getKey();
            Role role = entry.getValue();
            if (role != Role.SURVIVOR || !player.isOnline()) continue;

            double dist = player.getLocation().distance(slenderLoc);
            UUID uuid = player.getUniqueId();

            if (dist <= interferenceRange) {
                // Apply static interference
                staticInterference.put(uuid, System.currentTimeMillis());

                // Visual distortion
                if (ThreadLocalRandom.current().nextInt(100) < 30) {
                    spawnStaticParticles(player);
                }

                // Audio interference
                if (ThreadLocalRandom.current().nextInt(100) < 20) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.2f);
                }

                // Blindness flicker in Phase 3
                if (currentPhase == Phase.HUNTER && ThreadLocalRandom.current().nextInt(100) < 15) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                }
            } else {
                staticInterference.remove(uuid);
            }
        }
    }

    private void spawnStaticParticles(Player player) {
        Location loc = player.getLocation().add(0, 1.6, 0);
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i) / 8;
            double x = Math.cos(angle) * 0.5;
            double z = Math.sin(angle) * 0.5;
            loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }

    private void showNoiseAlerts(Player slender) {
        StealthManager stealth = arena.getStealthManager();
        if (stealth == null) return;

        for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
            Player player = entry.getKey();
            Role role = entry.getValue();
            if (role != Role.SURVIVOR || !player.isOnline()) continue;

            double noise = stealth.getNoise(player);
            if (noise > 3.0) {
                UUID uuid = player.getUniqueId();
                if (!noiseAlerts.contains(uuid)) {
                    noiseAlerts.add(uuid);
                    // Show glowing particles at player location for SlenderMan
                    Location pLoc = player.getLocation();
                    slender.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, pLoc.clone().add(0, 1, 0), 20, 0.5, 1, 0.5, 0);
                    slender.getWorld().spawnParticle(Particle.FLAME, pLoc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
                }
            } else {
                noiseAlerts.remove(player.getUniqueId());
            }
        }
    }

    private void checkJumpscareConditions(Player slender) {
        Location slenderLoc = slender.getLocation();

        for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
            Player player = entry.getKey();
            Role role = entry.getValue();
            if (role != Role.SURVIVOR || !player.isOnline()) continue;

            double dist = player.getLocation().distance(slenderLoc);

            // Jumpscare on collision (very close proximity)
            if (dist <= 1.2) {
                triggerJumpscare(player, slender);
                return;
            }

            // Jumpscare if sanity reaches 0 and player is looking at SlenderMan
            SanityManager sanity = arena.getSanityManager();
            if (sanity != null && sanity.getSanity(player) <= 0 && dist <= 8.0) {
                if (canSeePlayer(player, slender)) {
                    triggerJumpscare(player, slender);
                    return;
                }
            }
        }
    }

    private void triggerJumpscare(Player victim, Player slender) {
        // Screen goes black
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 10));

        // Loud scary sound
        victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2f, 0.3f);
        victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 2f, 0.5f);

        // Title
        victim.sendTitle("§c§lJUMPSCARE", "§7The SlenderMan got you...", 5, 40, 10);

        // Kill after delay
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
            if (victim.isOnline()) {
                victim.setHealth(0);
            }
        }, 40L);
    }

    private void teleportToNearestSurvivor(Player slender) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
            Player player = entry.getKey();
            Role role = entry.getValue();
            if (role != Role.SURVIVOR || !player.isOnline()) continue;

            double dist = slender.getLocation().distance(player.getLocation());
            if (dist < minDist && dist > 10) { // Don't teleport too close
                minDist = dist;
                nearest = player;
            }
        }

        if (nearest != null) {
            Location behind = nearest.getLocation().clone().add(
                    nearest.getLocation().getDirection().multiply(-5).setY(0));
            behind.setY(nearest.getLocation().getY());

            // Teleport with particle effect
            slender.getWorld().spawnParticle(Particle.PORTAL, slender.getLocation(), 30, 0.5, 1, 0.5, 0);
            slender.teleport(behind);
            slender.getWorld().spawnParticle(Particle.PORTAL, behind, 30, 0.5, 1, 0.5, 0);
            slender.playSound(slender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
    }

    private boolean canSeePlayer(Player observer, Player target) {
        org.bukkit.util.BlockIterator iterator = new org.bukkit.util.BlockIterator(
                observer.getWorld(), observer.getEyeLocation().toVector(),
                target.getEyeLocation().clone().subtract(observer.getEyeLocation()).toVector().normalize(),
                0, (int) observer.getEyeLocation().distance(target.getEyeLocation()));
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            if (block.getType().isOccluding()) return false;
        }
        return true;
    }

    public Phase getCurrentPhase() { return currentPhase; }

    public double getSpeedBonus() { return speedBonus; }
    public double getDamageBonus() { return damageBonus; }
    public int getDetectionRange() { return detectionRange; }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        staticInterference.clear();
        noiseAlerts.clear();
    }
}

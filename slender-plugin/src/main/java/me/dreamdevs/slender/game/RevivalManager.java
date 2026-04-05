package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.api.events.SlenderKillSurvivorEvent;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the full revival lifecycle: Downed → Revival → Recovery
 * Also handles Slenderman's execution mechanic on downed players.
 */
public class RevivalManager {

    private final Arena arena;

    // Downed player state
    private final Map<UUID, BukkitTask> bleedoutTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bleedoutTimers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> revivalCounts = new ConcurrentHashMap<>();

    // Revival channel state
    private final Map<UUID, UUID> activeRevivals = new ConcurrentHashMap<>(); // downed → reviver
    private final Map<UUID, Integer> revivalProgress = new ConcurrentHashMap<>(); // downed → ticks elapsed
    private final Map<UUID, BukkitTask> revivalTasks = new ConcurrentHashMap<>();

    // Execution channel state
    private final Map<UUID, UUID> activeExecutions = new ConcurrentHashMap<>(); // downed → executor
    private final Map<UUID, Integer> executionProgress = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> executionTasks = new ConcurrentHashMap<>();

    // Recovery state
    private final Map<UUID, BukkitTask> recoveryTasks = new ConcurrentHashMap<>();

    // Crawling animation: barrier blocks placed above downed players
    private final Map<UUID, Location> barrierLocations = new ConcurrentHashMap<>();

    public RevivalManager(Arena arena) {
        this.arena = arena;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DOWNED STATE
    // ═══════════════════════════════════════════════════════════════════

    public boolean isDowned(Player player) {
        return bleedoutTasks.containsKey(player.getUniqueId());
    }

    public boolean isBeingRevived(Player player) {
        return activeRevivals.containsKey(player.getUniqueId());
    }

    public boolean isBeingExecuted(Player player) {
        return activeExecutions.containsKey(player.getUniqueId());
    }

    public boolean canBeRevived(Player player) {
        int maxRevivals = Config.REVIVAL_MAX_PER_GAME.toInt();
        int used = revivalCounts.getOrDefault(player.getUniqueId(), 0);
        return used < maxRevivals;
    }

    /**
     * Transition a survivor into the DOWNED state.
     * Called instead of instant death when revival is enabled.
     */
    public void downPlayer(Player victim) {
        UUID uuid = victim.getUniqueId();

        // Check if they've used all their revivals
        if (!canBeRevived(victim)) {
            return; // Let normal death proceed
        }

        // Set role to DOWNED
        arena.getPlayers().put(victim, Role.DOWNED);

        // Prevent actual death: set to low health
        victim.setHealth(Math.min(victim.getHealth(), 6.0));

        // Force crawling pose by placing a barrier above the player's head
        placeCrawlBarrier(victim);

        // Immobilize
        victim.addPotionEffect(new PotionEffect(
                me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"),
                Integer.MAX_VALUE, 255, false, false, false));

        // Visual feedback: title
        Title downedTitle = Title.title(
                ColourUtil.colorizeToComponent(Langauge.REVIVAL_DOWNED_TITLE.toString()),
                ColourUtil.colorizeToComponent(Langauge.REVIVAL_DOWNED_SUBTITLE.toString()),
                Title.Times.times(Ticks.duration(5), Ticks.duration(60), Ticks.duration(20))
        );
        victim.showTitle(downedTitle);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);

        // Notify teammates
        String teamMsg = Langauge.REVIVAL_TEAMMATE_DOWNED.toString()
                .replace("%PLAYER%", victim.getName());
        arena.getPlayers().entrySet().stream()
                .filter(e -> e.getValue() == Role.SURVIVOR)
                .map(Map.Entry::getKey)
                .forEach(p -> {
                    p.sendMessage(ColourUtil.colorizeToComponent(teamMsg));
                    p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.2f);
                });

        // Start bleedout timer
        int bleedoutSeconds = Config.REVIVAL_BLEEDOUT_SECONDS.toInt();
        bleedoutTimers.put(uuid, bleedoutSeconds);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!victim.isOnline() || !isDowned(victim)) {
                    cancel();
                    return;
                }

                int timeLeft = bleedoutTimers.getOrDefault(uuid, 0);

                if (timeLeft <= 0) {
                    bleedOut(victim);
                    cancel();
                    return;
                }

                // Drain health slowly (0.5 hearts per tick = 1 HP per second)
                if (victim.getHealth() > 1.0) {
                    victim.setHealth(Math.max(1.0, victim.getHealth() - 1.0));
                }

                // Keep crawling pose active (update barrier position)
                updateCrawlBarrier(victim);

                // Action bar with timer
                if (!isBeingRevived(victim) && !isBeingExecuted(victim)) {
                    String actionBar = Langauge.REVIVAL_DOWNED_ACTIONBAR.toString()
                            .replace("%TIME%", String.valueOf(timeLeft));
                    victim.sendActionBar(ColourUtil.colorizeToComponent(actionBar));
                }

                // Bleeding particles
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                        victim.getLocation().add(0, 0.3, 0), 3, 0.3, 0.1, 0.3, 0.02);
                victim.getWorld().spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA,
                        victim.getLocation().add(0, 0.5, 0), 2, 0.2, 0.1, 0.2, 0);

                // Heartbeat sound every 2 seconds
                if (timeLeft % 2 == 0) {
                    victim.playSound(victim.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
                }

                // Red vignette pulse every 3 seconds
                if (timeLeft % 3 == 0) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8, 0, false, false, false));
                }

                bleedoutTimers.put(uuid, timeLeft - 1);
            }
        }.runTaskTimer(SlenderMain.getInstance(), 0L, 20L);

        bleedoutTasks.put(uuid, task);
    }

    /**
     * Player bled out completely — permanent death.
     */
    private void bleedOut(Player victim) {
        cleanupDownedState(victim);

        // Set to spectator
        arena.getPlayers().put(victim, Role.SPECTATOR);

        // Announce
        String msg = Langauge.REVIVAL_BLEEDOUT_DEATH.toString()
                .replace("%PLAYER%", victim.getName());
        arena.sendMessage(msg);

        // Kill effects
        victim.getWorld().strikeLightningEffect(victim.getLocation());
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 0.5f);

        // Give XP to Slenderman
        GamePlayer slenderGP = SlenderMain.getInstance().getPlayerManager().getPlayer(arena.getSlenderMan());
        GamePlayer victimGP = SlenderMain.getInstance().getPlayerManager().getPlayer(victim);
        if (slenderGP != null) {
            SlenderMain.getInstance().getLevelManager().addExp(slenderGP, 5);
            slenderGP.setStatistic(Statistic.KILLED_SURVIVORS, slenderGP.getStatistic(Statistic.KILLED_SURVIVORS) + 1);
            if (victimGP != null) {
                SlenderKillSurvivorEvent event = new SlenderKillSurvivorEvent(slenderGP, victimGP, arena);
                Bukkit.getPluginManager().callEvent(event);
            }
        }

        // Transition to spectator mode
        transitionToSpectator(victim);

        // Check win condition
        if (arena.getSurvivorsAmount() == 0) {
            arena.endGame(Role.SLENDER);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REVIVAL CHANNEL (Survivor reviving teammate)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Start a revival channel between reviver and downed player.
     */
    public void startRevival(Player reviver, Player downed) {
        UUID downedId = downed.getUniqueId();

        // Cancel any active execution
        if (isBeingExecuted(downed)) {
            cancelExecution(downed);
        }

        activeRevivals.put(downedId, reviver.getUniqueId());
        revivalProgress.put(downedId, 0);

        int totalTicks = Config.REVIVAL_DURATION_SECONDS.toInt() * 20;
        double maxDist = Config.REVIVAL_REVIVER_MAX_DISTANCE.toInt();

        // Slow down reviver during channel
        reviver.addPotionEffect(new PotionEffect(
                me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"),
                totalTicks + 20, 1, false, false, false));

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!reviver.isOnline() || !downed.isOnline() || !isDowned(downed)) {
                    cancelRevival(downed);
                    cancel();
                    return;
                }

                // Distance check
                if (reviver.getLocation().distance(downed.getLocation()) > maxDist) {
                    cancelRevival(downed);
                    reviver.sendMessage(ColourUtil.colorizeToComponent(Langauge.REVIVAL_CANCELLED.toString()));
                    downed.sendMessage(ColourUtil.colorizeToComponent(Langauge.REVIVAL_CANCELLED.toString()));
                    cancel();
                    return;
                }

                ticks++;
                int percent = Math.min(100, (ticks * 100) / totalTicks);
                String bar = buildProgressBar(percent, "&a", "&7");

                // Action bars for both players
                String reviverBar = Langauge.REVIVAL_REVIVING_ACTIONBAR.toString()
                        .replace("%BAR%", bar).replace("%PERCENT%", String.valueOf(percent));
                String downedBar = Langauge.REVIVAL_BEING_REVIVED_ACTIONBAR.toString()
                        .replace("%BAR%", bar).replace("%PERCENT%", String.valueOf(percent));

                reviver.sendActionBar(ColourUtil.colorizeToComponent(reviverBar));
                downed.sendActionBar(ColourUtil.colorizeToComponent(downedBar));

                // Particles between them
                if (ticks % 5 == 0) {
                    Location mid = reviver.getLocation().add(downed.getLocation()).multiply(0.5).add(0, 1, 0);
                    downed.getWorld().spawnParticle(Particle.HEART, mid, 2, 0.3, 0.3, 0.3, 0);
                    downed.getWorld().spawnParticle(Particle.COMPOSTER, downed.getLocation().add(0, 0.5, 0), 5, 0.3, 0.2, 0.3, 0);
                }

                // Sound tick
                if (ticks % 20 == 0) {
                    downed.playSound(downed.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                    reviver.playSound(reviver.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                }

                // Complete
                if (ticks >= totalTicks) {
                    completeRevival(reviver, downed);
                    cancel();
                }
            }
        }.runTaskTimer(SlenderMain.getInstance(), 0L, 1L);

        revivalTasks.put(downedId, task);
    }

    public void cancelRevival(Player downed) {
        UUID downedId = downed.getUniqueId();
        BukkitTask task = revivalTasks.remove(downedId);
        if (task != null) task.cancel();

        UUID reviverId = activeRevivals.remove(downedId);
        revivalProgress.remove(downedId);

        // Remove slowness from reviver
        if (reviverId != null) {
            Player reviver = Bukkit.getPlayer(reviverId);
            if (reviver != null && reviver.isOnline()) {
                reviver.removePotionEffect(
                        me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"));
            }
        }
    }

    private void completeRevival(Player reviver, Player downed) {
        UUID downedId = downed.getUniqueId();

        // Clean up revival state
        revivalTasks.remove(downedId);
        activeRevivals.remove(downedId);
        revivalProgress.remove(downedId);

        // Clean up downed state
        cleanupDownedState(downed);

        // Track revival count
        revivalCounts.merge(downedId, 1, Integer::sum);

        // Restore to survivor
        arena.getPlayers().put(downed, Role.SURVIVOR);

        // Remove reviver slowness
        reviver.removePotionEffect(
                me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"));

        // Recovery phase
        int recoveryHealth = Config.REVIVAL_RECOVERY_HEALTH.toInt();
        downed.setHealth(Math.min(recoveryHealth, 20));

        // Title
        Title reviveTitle = Title.title(
                ColourUtil.colorizeToComponent(Langauge.REVIVAL_SUCCESS_TITLE.toString()),
                ColourUtil.colorizeToComponent(Langauge.REVIVAL_SUCCESS_SUBTITLE.toString()),
                Title.Times.times(Ticks.duration(5), Ticks.duration(60), Ticks.duration(20))
        );
        downed.showTitle(reviveTitle);
        downed.playSound(downed.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        reviver.playSound(reviver.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        // Reviver feedback
        String reviverMsg = Langauge.REVIVAL_REVIVER_SUCCESS.toString()
                .replace("%PLAYER%", downed.getName());
        reviver.sendMessage(ColourUtil.colorizeToComponent(reviverMsg));

        // Give XP to reviver
        GamePlayer reviverGP = SlenderMain.getInstance().getPlayerManager().getPlayer(reviver);
        if (reviverGP != null) {
            SlenderMain.getInstance().getLevelManager().addExp(reviverGP, 8);
        }

        // Particles burst
        downed.getWorld().spawnParticle(Particle.HEART, downed.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
        downed.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, downed.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.2);

        // Warn if no more revivals
        if (!canBeRevived(downed)) {
            downed.sendMessage(ColourUtil.colorizeToComponent(Langauge.REVIVAL_NO_MORE_REVIVES.toString()));
        }

        // Start wounded recovery
        startRecovery(downed);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  EXECUTION CHANNEL (Slenderman executing downed player)
    // ═══════════════════════════════════════════════════════════════════

    public void startExecution(Player executor, Player downed) {
        UUID downedId = downed.getUniqueId();

        // Cancel any active revival
        if (isBeingRevived(downed)) {
            cancelRevival(downed);
        }

        activeExecutions.put(downedId, executor.getUniqueId());
        executionProgress.put(downedId, 0);

        int totalTicks = Config.REVIVAL_EXECUTION_SECONDS.toInt() * 20;
        double maxDist = Config.REVIVAL_REVIVER_MAX_DISTANCE.toInt();

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!executor.isOnline() || !downed.isOnline() || !isDowned(downed)) {
                    cancelExecution(downed);
                    cancel();
                    return;
                }

                // Distance check
                if (executor.getLocation().distance(downed.getLocation()) > maxDist) {
                    cancelExecution(downed);
                    cancel();
                    return;
                }

                ticks++;
                int percent = Math.min(100, (ticks * 100) / totalTicks);
                String bar = buildProgressBar(percent, "&4", "&8");

                // Action bars
                String execBar = Langauge.REVIVAL_EXECUTION_ACTIONBAR.toString()
                        .replace("%BAR%", bar).replace("%PERCENT%", String.valueOf(percent));
                String victimBar = Langauge.REVIVAL_EXECUTION_VICTIM_ACTIONBAR.toString()
                        .replace("%BAR%", bar);

                executor.sendActionBar(ColourUtil.colorizeToComponent(execBar));
                downed.sendActionBar(ColourUtil.colorizeToComponent(victimBar));

                // Ominous particles
                if (ticks % 5 == 0) {
                    downed.getWorld().spawnParticle(Particle.SOUL,
                            downed.getLocation().add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0.02);
                    downed.getWorld().spawnParticle(Particle.SMOKE,
                            downed.getLocation().add(0, 0.3, 0), 5, 0.3, 0.1, 0.3, 0.01);
                }

                // Ominous sound
                if (ticks % 20 == 0) {
                    downed.playSound(downed.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
                    executor.playSound(executor.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
                }

                // Complete execution
                if (ticks >= totalTicks) {
                    completeExecution(executor, downed);
                    cancel();
                }
            }
        }.runTaskTimer(SlenderMain.getInstance(), 0L, 1L);

        executionTasks.put(downedId, task);
    }

    public void cancelExecution(Player downed) {
        UUID downedId = downed.getUniqueId();
        BukkitTask task = executionTasks.remove(downedId);
        if (task != null) task.cancel();
        activeExecutions.remove(downedId);
        executionProgress.remove(downedId);
    }

    private void completeExecution(Player executor, Player victim) {
        UUID victimId = victim.getUniqueId();
        executionTasks.remove(victimId);
        activeExecutions.remove(victimId);
        executionProgress.remove(victimId);

        // Clean up downed state
        cleanupDownedState(victim);

        // Set to spectator
        arena.getPlayers().put(victim, Role.SPECTATOR);

        // Announce execution
        String msg = Langauge.REVIVAL_EXECUTION_COMPLETE.toString()
                .replace("%PLAYER%", victim.getName());
        arena.sendMessage(msg);

        // Dramatic effects
        victim.getWorld().strikeLightningEffect(victim.getLocation());
        victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);

        // Stats
        GamePlayer slenderGP = SlenderMain.getInstance().getPlayerManager().getPlayer(executor);
        GamePlayer victimGP = SlenderMain.getInstance().getPlayerManager().getPlayer(victim);
        if (slenderGP != null) {
            SlenderMain.getInstance().getLevelManager().addExp(slenderGP, 7);
            slenderGP.setStatistic(Statistic.KILLED_SURVIVORS, slenderGP.getStatistic(Statistic.KILLED_SURVIVORS) + 1);
            if (victimGP != null) {
                SlenderKillSurvivorEvent event = new SlenderKillSurvivorEvent(slenderGP, victimGP, arena);
                Bukkit.getPluginManager().callEvent(event);
            }
        }

        // Transition to spectator mode
        transitionToSpectator(victim);

        // Check win condition
        if (arena.getSurvivorsAmount() == 0) {
            arena.endGame(Role.SLENDER);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RECOVERY PHASE
    // ═══════════════════════════════════════════════════════════════════

    private void startRecovery(Player player) {
        UUID uuid = player.getUniqueId();
        int recoverySeconds = Config.REVIVAL_RECOVERY_SECONDS.toInt();

        // Wounded effects: slowness + brief red screen
        player.addPotionEffect(new PotionEffect(
                me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"),
                recoverySeconds * 20, 0, false, false, false));

        // Blood screen pulse for first 3 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, false, false, false));

        double maxHealth = 20.0;
        double startHealth = Config.REVIVAL_RECOVERY_HEALTH.toInt();
        double healPerTick = (maxHealth - startHealth) / (recoverySeconds * 20.0);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = recoverySeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline() || arena.getPlayers().get(player) != Role.SURVIVOR) {
                    cancel();
                    recoveryTasks.remove(uuid);
                    return;
                }

                ticks++;

                // Gradual healing
                double newHealth = Math.min(maxHealth, startHealth + (healPerTick * ticks));
                if (player.getHealth() < newHealth) {
                    player.setHealth(newHealth);
                }

                // Blood pulse effect in first 3 seconds
                if (ticks < 60 && ticks % 20 == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8, 0, false, false, false));
                    player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                            player.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                }

                if (ticks >= totalTicks) {
                    player.setHealth(maxHealth);
                    player.removePotionEffect(
                            me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"));
                    cancel();
                    recoveryTasks.remove(uuid);
                }
            }
        }.runTaskTimer(SlenderMain.getInstance(), 0L, 1L);

        recoveryTasks.put(uuid, task);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════════════

    public void cleanupDownedState(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel bleedout task
        BukkitTask bleedTask = bleedoutTasks.remove(uuid);
        if (bleedTask != null) bleedTask.cancel();
        bleedoutTimers.remove(uuid);

        // Cancel any active revival
        cancelRevival(player);

        // Cancel any active execution
        cancelExecution(player);

        // Remove crawling barrier
        removeCrawlBarrier(player);
        player.removePotionEffect(
                me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"));
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    private void transitionToSpectator(Player player) {
        // Title
        Title deathTitle = Title.title(
                ColourUtil.colorizeToComponent(Langauge.ARENA_DEAD_TITLE.toString()),
                ColourUtil.colorizeToComponent(Langauge.ARENA_DEAD_SUBTITLE.toString()),
                Title.Times.times(Ticks.duration(5), Ticks.duration(60), Ticks.duration(20))
        );
        player.showTitle(deathTitle);

        // Spectator setup
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
            player.setGlowing(false);
            player.sendMessage(ColourUtil.colorizeToComponent(Langauge.ARENA_SPECTATOR_MODE.toString()));

            // Hide from non-spectators
            arena.getPlayers().entrySet().stream()
                    .filter(e -> e.getValue() != Role.SPECTATOR)
                    .map(Map.Entry::getKey)
                    .forEach(p -> p.hidePlayer(SlenderMain.getInstance(), player));

            // Spectator effects
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, Integer.MAX_VALUE));
            player.getInventory().clear();
            player.getInventory().setItem(0, CustomItem.SPECTATOR_TELEPORTER.toItemStack());
            player.getInventory().setItem(4, CustomItem.SPECTATOR_SETTINGS.toItemStack());
            player.getInventory().setItem(7, CustomItem.PLAY_AGAIN.toItemStack());
            player.getInventory().setItem(8, CustomItem.LEAVE.toItemStack());
            player.setAllowFlight(true);
            player.setFlying(true);

            // Teleport to slenderman spawn (spectator area)
            if (arena.getSlenderManSpawnLocation() != null) {
                player.teleport(arena.getSlenderManSpawnLocation());
            }
        }, 10L);
    }

    /**
     * Build a visual progress bar like: ████████░░░░
     */
    private String buildProgressBar(int percent, String filledColor, String emptyColor) {
        int totalBars = 20;
        int filled = (percent * totalBars) / 100;
        int empty = totalBars - filled;

        StringBuilder sb = new StringBuilder();
        sb.append(filledColor);
        for (int i = 0; i < filled; i++) sb.append("█");
        sb.append(emptyColor);
        for (int i = 0; i < empty; i++) sb.append("░");
        return sb.toString();
    }

    /**
     * Place an invisible barrier above the player to force crawling pose.
     * When there's a solid block just above the player's head,
     * the game forces them into the swimming/crawling animation.
     */
    private void placeCrawlBarrier(Player player) {
        UUID uuid = player.getUniqueId();
        // Remove old barrier if any
        removeCrawlBarrier(player);

        Location barrierLoc = player.getLocation().getBlock().getRelative(0, 1, 0).getLocation();
        Block block = barrierLoc.getBlock();

        // Only place if the block is air (don't overwrite existing blocks)
        if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
            block.setType(Material.BARRIER, false);
            barrierLocations.put(uuid, barrierLoc.clone());
        }
    }

    /**
     * Update the crawling barrier as the player moves (though they barely can).
     */
    private void updateCrawlBarrier(Player player) {
        UUID uuid = player.getUniqueId();
        Location current = player.getLocation().getBlock().getRelative(0, 1, 0).getLocation();
        Location stored = barrierLocations.get(uuid);

        // If player hasn't moved to a different block, keep existing barrier
        if (stored != null && stored.getBlockX() == current.getBlockX()
                && stored.getBlockY() == current.getBlockY()
                && stored.getBlockZ() == current.getBlockZ()) {
            return;
        }

        // Player moved — relocate barrier
        placeCrawlBarrier(player);
    }

    /**
     * Remove the crawling barrier when player stands up or dies.
     */
    private void removeCrawlBarrier(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = barrierLocations.remove(uuid);
        if (loc != null && loc.getWorld() != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR, false);
            }
        }
    }

    /**
     * Stop all tasks — called during arena restart.
     */
    public void stop() {
        bleedoutTasks.values().forEach(BukkitTask::cancel);
        bleedoutTasks.clear();
        bleedoutTimers.clear();

        revivalTasks.values().forEach(BukkitTask::cancel);
        revivalTasks.clear();
        activeRevivals.clear();
        revivalProgress.clear();

        executionTasks.values().forEach(BukkitTask::cancel);
        executionTasks.clear();
        activeExecutions.clear();
        executionProgress.clear();

        recoveryTasks.values().forEach(BukkitTask::cancel);
        recoveryTasks.clear();

        // Clean up all barrier blocks
        for (Location loc : barrierLocations.values()) {
            if (loc != null && loc.getWorld() != null) {
                Block block = loc.getBlock();
                if (block.getType() == Material.BARRIER) {
                    block.setType(Material.AIR, false);
                }
            }
        }
        barrierLocations.clear();

        revivalCounts.clear();
    }
}

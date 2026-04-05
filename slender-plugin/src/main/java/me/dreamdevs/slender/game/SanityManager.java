package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.perks.Resilience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sanity system: tracks player mental state (0-100).
 * Drains when looking at SlenderMan, being in darkness, or paranormal events.
 * Low sanity causes visual distortion, panic attacks, and vulnerability.
 * Displays sanity via BossBar (white→yellow→red based on danger).
 */
public class SanityManager {

    private final Map<UUID, Double> sanityLevels = new HashMap<>();
    private final Map<UUID, BossBar> sanityBars = new HashMap<>();
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
                if (role != Role.SURVIVOR) continue;
                if (!player.isOnline()) continue;

                UUID uuid = player.getUniqueId();
                double sanity = sanityLevels.getOrDefault(uuid, 100.0);

                // Get player's equipped perk for sanity drain reduction (Resilience)
                GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
                double drainMultiplier = 1.0;
                if (gp != null) {
                    Perk perk = gp.getPerk(Role.SURVIVOR);
                    if (perk instanceof Resilience) {
                        drainMultiplier = ((Resilience) perk).getDrainMultiplier(); // 0.85
                    }
                }

                // Drain sanity when looking at SlenderMan within 10 blocks
                if (player.getWorld().equals(slenderLoc.getWorld())) {
                    double dist = player.getLocation().distance(slenderLoc);
                    if (dist <= 10.0 && canSeePlayer(player, slender)) {
                        // Panic attack if health > 70% and looking directly
                        double healthPercent = player.getHealth() / player.getAttribute(me.dreamdevs.slender.utils.AttributeUtils.getMaxHealth()).getValue();
                        if (healthPercent > 0.7 && dist <= 5.0) {
                            triggerPanicAttack(player, arena);
                            sanity -= 15.0 * drainMultiplier;
                        } else {
                            sanity -= 3.0 * drainMultiplier;
                        }
                    }
                }

                // Drain sanity in darkness (light level 0)
                if (player.getLocation().getBlock().getLightLevel() == 0) {
                    sanity -= 0.5 * drainMultiplier;
                }

                // Sneaking stabilizes sanity
                if (player.isSneaking()) {
                    sanity = Math.min(100.0, sanity + 0.3);
                }

                // Natural slow recovery in light
                if (player.getLocation().getBlock().getLightLevel() > 7) {
                    sanity = Math.min(100.0, sanity + 0.2);
                }

                sanity = Math.max(0.0, Math.min(100.0, sanity));
                sanityLevels.put(uuid, sanity);

                // Update sanity BossBar
                updateSanityBar(player, sanity);

                // Apply effects based on sanity level
                applySanityEffects(player, sanity);

                // Action bar: stealth noise warning
                StealthManager stealth = arena.getStealthManager();
                if (stealth != null) {
                    double noise = stealth.getNoise(player);
                    if (noise > 5.0) {
                        player.sendActionBar(Component.text("⚠ You're making too much noise...", NamedTextColor.RED));
                    } else if (noise > 2.0) {
                        player.sendActionBar(Component.text("You're being heard...", NamedTextColor.YELLOW));
                    }
                }
            }
        }, 0L, 40L);
    }

    private void updateSanityBar(Player player, double sanity) {
        BossBar bar = sanityBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar b = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
            b.addPlayer(player);
            return b;
        });

        bar.setProgress(sanity / 100.0);

        // Color based on danger level
        if (sanity > 70) {
            bar.setColor(BarColor.WHITE);
            bar.setTitle("Sanity: " + (int) sanity + "%");
        } else if (sanity > 40) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle("Sanity: " + (int) sanity + "%");
        } else if (sanity > 15) {
            bar.setColor(BarColor.RED);
            bar.setTitle("⚠ Sanity: " + (int) sanity + "%");
        } else {
            bar.setColor(BarColor.RED);
            bar.setTitle("☠ CRITICAL: " + (int) sanity + "%");
        }
    }

    private void triggerPanicAttack(Player player, Arena arena) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
        
        Title title = Title.title(
                Component.text("PANIC ATTACK", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("You can't control your fear...", NamedTextColor.GRAY),
                Title.Times.times(Ticks.duration(10), Ticks.duration(60), Ticks.duration(20))
        );
        player.showTitle(title);
    }

    private void applySanityEffects(Player player, double sanity) {
        if (sanity <= 0) {
            // Maximum vulnerability
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
            if (ThreadLocalRandom.current().nextInt(100) < 5) {
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.5f, 0.3f);
            }
        } else if (sanity <= 25) {
            // Severe distortion
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
            if (ThreadLocalRandom.current().nextInt(100) < 10) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.4f);
            }
        } else if (sanity <= 50) {
            // Intermittent blindness
            if (ThreadLocalRandom.current().nextInt(100) < 15) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
            }
            if (ThreadLocalRandom.current().nextInt(100) < 8) {
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.2f, 0.6f);
            }
        } else {
            // Clear mind - remove negative effects
            player.removePotionEffect(PotionEffectType.NAUSEA);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private boolean canSeePlayer(Player observer, Player target) {
        if (!observer.getWorld().equals(target.getWorld())) return false;
        Location from = observer.getEyeLocation();
        Location to = target.getEyeLocation();
        double distance = from.distance(to);
        if (distance < 1.0) return true;

        org.bukkit.util.Vector direction = to.clone().subtract(from).toVector();
        if (direction.lengthSquared() == 0.0) return true;

        org.bukkit.util.BlockIterator iterator = new org.bukkit.util.BlockIterator(
                observer.getWorld(), from.toVector(), direction.normalize(), 0, (int) distance);
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            if (block.getType().isOccluding()) return false;
        }
        return true;
    }

    public double getSanity(Player player) {
        return sanityLevels.getOrDefault(player.getUniqueId(), 100.0);
    }

    public void setSanity(Player player, double value) {
        sanityLevels.put(player.getUniqueId(), Math.max(0.0, Math.min(100.0, value)));
    }

    public void drainSanity(Player player, double amount) {
        double current = getSanity(player);
        setSanity(player, current - amount);
    }

    public void restoreSanity(Player player, double amount) {
        double current = getSanity(player);
        setSanity(player, current + amount);
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = sanityBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        sanityLevels.remove(uuid);
        // Clear effects
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        sanityBars.values().forEach(BossBar::removeAll);
        sanityBars.clear();
        sanityLevels.clear();
    }
}

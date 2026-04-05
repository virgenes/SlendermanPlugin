package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.SlenderKillSurvivorEvent;
import me.dreamdevs.slender.api.events.SlenderSurvivorPickupPageEvent;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.CustomItem;
import me.dreamdevs.slender.game.RevivalManager;
import me.dreamdevs.slender.game.perks.PrayerSpeed;
import me.dreamdevs.slender.game.perks.Spirit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.Location;

import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameListeners implements Listener {

    private final Map<UUID, BukkitTask> activeCollections = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> swordCooldowns = new ConcurrentHashMap<>();

    @EventHandler
    public void damageEvent(EntityDamageByEntityEvent event) {
        if(!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player))
            return;

        Player damager = (Player) event.getDamager();
        Player entity = (Player) event.getEntity();

        GamePlayer attackerPlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(damager);
        GamePlayer victimPlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(entity);

        if (attackerPlayer == null || victimPlayer == null) {
            event.setCancelled(true);
            return;
        }

        // Cancel collection if victim is hit
        cancelCollection(entity.getUniqueId());

        if(!attackerPlayer.isInArena() || !victimPlayer.isInArena()) {
            event.setCancelled(true);
            return;
        }

        Arena arena = (Arena) attackerPlayer.getArena();
        if (arena == null) {
            event.setCancelled(true);
            return;
        }

        Role attackerRole = arena.getPlayers().get(damager);
        Role victimRole = arena.getPlayers().get(entity);

        if(attackerRole == Role.SPECTATOR || attackerRole == Role.NONE || attackerRole == Role.DOWNED) {
            event.setCancelled(true);
            return;
        }

        // Block damage to/from downed players (except Slenderman executing)
        if (victimRole == Role.DOWNED) {
            if (attackerRole == Role.SLENDER) {
                // Slenderman hits a downed player → start execution channel
                RevivalManager rm = arena.getRevivalManager();
                if (rm != null && !rm.isBeingExecuted(entity)) {
                    rm.startExecution(damager, entity);
                }
            }
            event.setCancelled(true);
            return;
        }

        if(attackerRole == Role.SURVIVOR && victimRole == Role.SURVIVOR) {
            event.setCancelled(true);
        }
        if ((attackerRole == Role.PROXY && victimRole == Role.PROXY) ||
            (attackerRole == Role.PROXY && victimRole == Role.SLENDER) ||
            (attackerRole == Role.SLENDER && victimRole == Role.PROXY)) {
            event.setCancelled(true);
            return;
        }
        
        if (attackerRole == Role.SURVIVOR && victimRole == Role.SLENDER) {
            handleSurvivorSwordHit(damager, entity);
            event.setCancelled(true); // Always cancel actual damage to Slenderman
        }
        if (attackerRole == Role.SURVIVOR && victimRole == Role.PROXY) {
            // Survivors can deal damage to Proxies natively. Death handles it (they just respawn).
        }

        // Slenderman or Proxy attacking a survivor: check if this hit should down them
        if ((attackerRole == Role.SLENDER || attackerRole == Role.PROXY) && victimRole == Role.SURVIVOR) {
            // Trigger combat music for all arena players when Slenderman hits a survivor
            if (arena.getMusicManager() != null) {
                arena.getMusicManager().triggerCombat();
            }
            
            // Anti-combat-log hit tracking
            arena.markCombatHit(entity.getUniqueId());

            if (Config.REVIVAL_ENABLED.toBoolean() && arena.getRevivalManager() != null) {
                double damageAmount = event.getFinalDamage();
                if (entity.getHealth() - damageAmount <= 0) {
                    // Would kill → intercept and down instead
                    RevivalManager rm = arena.getRevivalManager();
                    if (rm.canBeRevived(entity)) {
                        event.setCancelled(true);
                        rm.downPlayer(entity);
                        return;
                    }
                }
            }
        }
    }

    public static void handleSurvivorSwordHit(Player damager, Player entity) {
        GamePlayer attackerPlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(damager);
        GamePlayer victimPlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(entity);
        if (attackerPlayer == null || victimPlayer == null || !attackerPlayer.isInArena() || !victimPlayer.isInArena()) return;

        Arena arena = (Arena) attackerPlayer.getArena();
        if (arena == null) return;

        org.bukkit.inventory.ItemStack hand = damager.getInventory().getItemInMainHand();
        if (hand.getType() == org.bukkit.Material.WOODEN_SWORD && hand.hasItemMeta()) {
            // Check Cooldown
            long now = System.currentTimeMillis();
            if (swordCooldowns.getOrDefault(damager.getUniqueId(), 0L) > now) {
                long timeLeft = (swordCooldowns.get(damager.getUniqueId()) - now) / 1000;
                damager.sendActionBar(ColourUtil.colorizeToComponent(
                        Langauge.SWORD_STUN_COOLDOWN.toString().replace("%TIME%", String.valueOf(timeLeft))));
                return;
            }

            org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
            NamespacedKey hitsKey = new NamespacedKey(SlenderMain.getInstance(), "sword_hits_left");
            int hitsLeft = meta.getPersistentDataContainer().getOrDefault(hitsKey, PersistentDataType.INTEGER, 3);
            
            if (hitsLeft > 0) {
                hitsLeft--;
                swordCooldowns.put(damager.getUniqueId(), now + 20000); // 20s cooldown
                
                // Stun Slenderman (5 seconds = 100 ticks)
                entity.addPotionEffect(new PotionEffect(me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"), 100, 255, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1, false, false));
                
                // Professional Feedback (with translations)
                Title stunTitle = Title.title(
                        ColourUtil.colorizeToComponent(Langauge.SWORD_STUN_KILLER_TITLE.toString()),
                        ColourUtil.colorizeToComponent(Langauge.SWORD_STUN_KILLER_SUBTITLE.toString().replace("%SECONDS%", "5")),
                        Title.Times.times(Ticks.duration(0), Ticks.duration(100), Ticks.duration(20))
                );
                entity.showTitle(stunTitle);
                entity.playSound(entity.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.8f);
                entity.getWorld().spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1, 0), 1);
                
                damager.sendActionBar(ColourUtil.colorizeToComponent(
                        Langauge.SWORD_STUN_SURVIVOR_ACTIONBAR.toString().replace("%SECONDS%", "5")));
                damager.playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1.2f);
                entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                
                // Trigger combat music for all arena players
                if (arena.getMusicManager() != null) {
                    arena.getMusicManager().triggerCombat();
                }
                
                if (hitsLeft <= 0) {
                    damager.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
                    damager.playSound(damager.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    damager.sendMessage(ColourUtil.colorizeToComponent(Langauge.SWORD_STUN_SWORD_BROKEN.toString()));
                } else {
                    meta.getPersistentDataContainer().set(hitsKey, PersistentDataType.INTEGER, hitsLeft);
                    List<Component> lore = new ArrayList<>();
                    lore.add(ColourUtil.colorizeToComponent(Langauge.SWORD_STUN_USES_LEFT.toString().replace("%HITS%", String.valueOf(hitsLeft))));
                    meta.lore(lore);
                    hand.setItemMeta(meta);
                    damager.sendMessage(ColourUtil.colorizeToComponent(
                            Langauge.SWORD_STUN_USES_LEFT.toString().replace("%HITS%", String.valueOf(hitsLeft))));
                }
            }
        }
    }


    @EventHandler
    public void deathPlayer(PlayerDeathEvent event) {
        event.deathMessage(null);
        event.setDroppedExp(0);
        event.setNewTotalExp(0);
        event.setNewLevel(0);
        event.getDrops().clear();

        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getEntity());
        if(gamePlayer == null || !gamePlayer.isInArena())
            return;
        Arena arena = (Arena) gamePlayer.getArena();

        Role currentRole = arena.getPlayers().get(gamePlayer.getPlayer());

        // DOWNED player bleeding out to death → handled by RevivalManager, just respawn as spectator
        if (currentRole == Role.DOWNED) {
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                gamePlayer.getPlayer().spigot().respawn();
            }, 4L);
            return;
        }

        if(currentRole == Role.SURVIVOR) {
            GamePlayer slender = SlenderMain.getInstance().getPlayerManager().getPlayer(arena.getSlenderMan());
            if (slender != null) {
                SlenderMain.getInstance().getLevelManager().addExp(slender, 5);
                SlenderKillSurvivorEvent slenderManKillSurvivorEvent = new SlenderKillSurvivorEvent(slender, gamePlayer, arena);
                Bukkit.getPluginManager().callEvent(slenderManKillSurvivorEvent);
                slender.setStatistic(Statistic.KILLED_SURVIVORS, slender.getStatistic(Statistic.KILLED_SURVIVORS)+1);
            }

            if (arena.getActiveMode() == me.dreamdevs.slender.api.game.GameMode.INFECTION) {
                arena.getPlayers().put(gamePlayer.getPlayer(), Role.PROXY);
            } else {
                arena.getPlayers().put(gamePlayer.getPlayer(), Role.SPECTATOR);
            }

            // Spirit perk: grant bonus to remaining allies
            Perk survivorPerk = gamePlayer.getPerk(Role.SURVIVOR);
            if (survivorPerk instanceof Spirit) {
                List<Player> allies = new ArrayList<>();
                arena.getPlayers().entrySet().stream()
                        .filter(e -> e.getValue() == Role.SURVIVOR)
                        .map(Map.Entry::getKey)
                        .filter(Player::isOnline)
                        .forEach(allies::add);
                if (!allies.isEmpty()) {
                    ((Spirit) survivorPerk).applyDeathBonus(gamePlayer.getPlayer(), allies);
                }
            }

            if(arena.getSurvivorsAmount() == 0) {
                arena.endGame(Role.SLENDER);
            } else if (arena.getSurvivorsAmount() == 1 && arena.getActiveMode() == me.dreamdevs.slender.api.game.GameMode.INFECTION) {
                // LAST HOPE BUFF
                Player lastHope = arena.getPlayers().entrySet().stream()
                        .filter(e -> e.getValue() == Role.SURVIVOR)
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);
                
                if (lastHope != null) {
                    lastHope.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                    lastHope.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
                    lastHope.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
                    
                    Title hopeTitle = Title.title(
                            ColourUtil.colorizeToComponent("&e&lLAST HOPE"),
                            ColourUtil.colorizeToComponent("&fYou are the only one left. Survive!"),
                            Title.Times.times(Ticks.duration(10), Ticks.duration(60), Ticks.duration(10))
                    );
                    lastHope.showTitle(hopeTitle);
                    lastHope.playSound(lastHope.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                }
            }

            event.getEntity().getLocation().getWorld().strikeLightningEffect(event.getEntity().getLocation());
            arena.sendMessage(Langauge.ARENA_KILLED_BY_SLENDER_MAN.toString().replace("%PLAYER%", gamePlayer.getPlayer().getName()));

            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                gamePlayer.getPlayer().spigot().respawn();
                gamePlayer.getPlayer().setGlowing(false);
                if (arena.getActiveMode() == me.dreamdevs.slender.api.game.GameMode.INFECTION) {
                    gamePlayer.getPlayer().sendMessage(ColourUtil.colorizeToComponent("&cYou have been assimilated into the Cult of Proxies! Hunt the remaining survivors!"));
                } else {
                    gamePlayer.getPlayer().sendMessage(ColourUtil.colorizeToComponent(Langauge.ARENA_SPECTATOR_MODE.toString()));
                }
            }, 4L);

        }

        if (gamePlayer.getPlayer().getKiller() == null)
            return;

        if(currentRole == Role.SLENDER) {
            arena.sendMessage(Langauge.ARENA_KILLED_BY_SURVIVOR.toString());
            GamePlayer killer = SlenderMain.getInstance().getPlayerManager().getPlayer(gamePlayer.getPlayer().getKiller());
            if (killer != null) {
                killer.setStatistic(Statistic.KILLED_SLENDERMEN, killer.getStatistic(Statistic.KILLED_SLENDERMEN)+1);
                SlenderMain.getInstance().getLevelManager().addExp(killer, 10);
            }
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> gamePlayer.getPlayer().spigot().respawn(), 4L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REVIVAL INTERACTION — Right-click a downed teammate to revive
    // ═══════════════════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;

        Player clicker = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        GamePlayer clickerGP = SlenderMain.getInstance().getPlayerManager().getPlayer(clicker);
        if (clickerGP == null || !clickerGP.isInArena()) return;

        Arena arena = (Arena) clickerGP.getArena();
        if (arena == null || arena.getArenaState() != ArenaState.RUNNING) return;

        Role clickerRole = arena.getPlayers().get(clicker);
        Role targetRole = arena.getPlayers().get(target);

        if (targetRole != Role.DOWNED) return;

        RevivalManager rm = arena.getRevivalManager();
        if (rm == null) return;

        // Survivor right-clicking a downed teammate → start revival
        if (clickerRole == Role.SURVIVOR) {
            if (!rm.isBeingRevived(target)) {
                rm.startRevival(clicker, target);
            }
            event.setCancelled(true);
        }

        // Slenderman right-clicking a downed player → start execution
        if (clickerRole == Role.SLENDER) {
            if (!rm.isBeingExecuted(target)) {
                rm.startExecution(clicker, target);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void respawnEvent(PlayerRespawnEvent event) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
        if(gamePlayer == null || !gamePlayer.isInArena())
            return;
        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null) return;

        Role role = arena.getPlayers().get(gamePlayer.getPlayer());
        if((role == Role.SURVIVOR || role == Role.SPECTATOR || role == Role.NONE) && (arena.getArenaState() == ArenaState.RUNNING || arena.getArenaState() == ArenaState.ENDING)) {
            event.setRespawnLocation(arena.getSlenderManSpawnLocation());
            arena.getPlayers().entrySet().stream().filter(playerRoleEntry -> playerRoleEntry.getValue() != Role.SPECTATOR).map(Map.Entry::getKey).forEach(player -> player.hidePlayer(SlenderMain.getInstance(), gamePlayer.getPlayer()));
            // Remove blindness and add night vision for spectators
            gamePlayer.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
            gamePlayer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, Integer.MAX_VALUE));
            gamePlayer.getPlayer().getInventory().clear();
            gamePlayer.getPlayer().getInventory().setItem(0, CustomItem.SPECTATOR_TELEPORTER.toItemStack());
            gamePlayer.getPlayer().getInventory().setItem(4, CustomItem.SPECTATOR_SETTINGS.toItemStack());
            gamePlayer.getPlayer().getInventory().setItem(7, CustomItem.PLAY_AGAIN.toItemStack());
            gamePlayer.getPlayer().getInventory().setItem(8, CustomItem.LEAVE.toItemStack());
            gamePlayer.getPlayer().setAllowFlight(true);
            gamePlayer.getPlayer().setFlying(true);
            
            Title title = Title.title(
                    ColourUtil.colorizeToComponent(Langauge.ARENA_DEAD_TITLE.toString()),
                    ColourUtil.colorizeToComponent(Langauge.ARENA_DEAD_SUBTITLE.toString()),
                    Title.Times.times(Ticks.duration(10), Ticks.duration(30), Ticks.duration(10))
            );
            gamePlayer.getPlayer().showTitle(title);
        } else if(role == Role.SLENDER) {
            event.setRespawnLocation(gamePlayer.getArena().getSlenderManSpawnLocation());
            Player p = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                p.getInventory().clear();
                p.getInventory().setItem(0, CustomItem.SLENDERMAN_WEAPON.toItemStack());
                p.getInventory().setItem(1, CustomItem.SLENDERMAN_COMPASS.toItemStack());
                p.getAttribute(me.dreamdevs.slender.utils.AttributeUtils.getMaxHealth()).setBaseValue(40);
                p.setHealth(40);
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, Integer.MAX_VALUE));
            }, 1L);
        } else if (role == Role.PROXY) {
            event.setRespawnLocation(gamePlayer.getArena().getSlenderManSpawnLocation()); // Proxies spawn near slender
            Player p = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                p.getInventory().clear();
                p.setGameMode(org.bukkit.GameMode.ADVENTURE);
                
                // Dark Leather Armor
                org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET);
                org.bukkit.inventory.meta.LeatherArmorMeta hMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
                hMeta.setColor(org.bukkit.Color.BLACK);
                hMeta.setUnbreakable(true);
                helmet.setItemMeta(hMeta);
                
                org.bukkit.inventory.ItemStack chest = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
                org.bukkit.inventory.meta.LeatherArmorMeta cMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) chest.getItemMeta();
                cMeta.setColor(org.bukkit.Color.BLACK);
                cMeta.setUnbreakable(true);
                chest.setItemMeta(cMeta);

                p.getInventory().setHelmet(helmet);
                p.getInventory().setChestplate(chest);

                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));

                // Visual Proxy Aura
                Title title = Title.title(
                        ColourUtil.colorizeToComponent("&c&kX &4&lPROXY &c&kX"),
                        ColourUtil.colorizeToComponent("&cThe Cult claims you."),
                        Title.Times.times(Ticks.duration(10), Ticks.duration(40), Ticks.duration(10))
                );
                p.showTitle(title);
            }, 1L);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        Material type = event.getClickedBlock().getType();
        if (type != Material.SKELETON_SKULL && type != Material.SKELETON_WALL_SKULL && 
            type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) return;
        
        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gamePlayer == null || !gamePlayer.isInArena()) return;
        
        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null || arena.getPlayers().get(player) != Role.SURVIVOR) return;
        
        // Check if this block is the current page (Comparing world name + integer coordinates)
        Location current = arena.getCurrentPageLocation();
        if (current == null ||
            !event.getClickedBlock().getWorld().getName().equals(current.getWorld().getName()) ||
            event.getClickedBlock().getX() != current.getBlockX() ||
            event.getClickedBlock().getY() != current.getBlockY() ||
            event.getClickedBlock().getZ() != current.getBlockZ()) {
            return;
        }
        
        player.sendActionBar(ColourUtil.colorizeToComponent(Langauge.ARENA_COLLECTION_START.toString()));
        startPageCollection(player, gamePlayer, arena);
        event.setCancelled(true);
    }

    private void startPageCollection(Player player, GamePlayer gamePlayer, Arena arena) {
        if (activeCollections.containsKey(player.getUniqueId())) return;

        // Calculate increment with perk bonus
        double baseIncrement = 2.0; // Per 2 ticks
        Perk perk = gamePlayer.getPerk(Role.SURVIVOR);
        if (perk instanceof PrayerSpeed) {
            baseIncrement *= 1.25; // 25% faster
        }
        final double finalIncrement = baseIncrement;

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            private double progress = 0;
            private final int maxProgress = 200; // 10 seconds (20 ticks * 10)

            @Override
            public void run() {
                if (!player.isOnline() || !gamePlayer.isInArena() || arena.getArenaState() != ArenaState.RUNNING) {
                    cancelCollection(player.getUniqueId());
                    return;
                }

                // Check distance and focus (Comparing block coordinates)
                Location current = arena.getCurrentPageLocation();
                if (current == null || player.getLocation().distance(current) > 4.5) {
                    player.sendActionBar(ColourUtil.colorizeToComponent(Langauge.ARENA_COLLECTION_TOO_FAR.toString()));
                    cancelCollection(player.getUniqueId());
                    return;
                }

                // Check focus (more lenient than raytracing)
                org.bukkit.util.Vector toPage = arena.getCurrentPageLocation().clone().add(0.5, 0.5, 0.5).toVector().subtract(player.getEyeLocation().toVector()).normalize();
                org.bukkit.util.Vector direction = player.getEyeLocation().getDirection();
                double dot = direction.dot(toPage);

                if (dot < 0.85) { // Roughly 30 degrees field of view
                    player.sendActionBar(ColourUtil.colorizeToComponent(Langauge.ARENA_COLLECTION_KEEP_LOOKING.toString()));
                    cancelCollection(player.getUniqueId());
                    return;
                }

                // Update progress
                progress += finalIncrement;
                if (progress >= maxProgress) {
                    collectPage(player, gamePlayer, arena);
                    player.sendActionBar(ColourUtil.colorizeToComponent(Langauge.ARENA_COLLECTION_COMPLETED.toString()));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    cancelCollection(player.getUniqueId());
                    return;
                }

                // Aesthetic Action Bar
                int percent = (int) ((progress / (double) maxProgress) * 100);
                String bar = getProgressBar((int)progress, maxProgress, 20);
                String prefix = perk instanceof PrayerSpeed ? Langauge.ARENA_COLLECTION_PERK_PREFIX.toString() : Langauge.ARENA_COLLECTION_PREFIX.toString();
                player.sendActionBar(ColourUtil.colorizeToComponent(prefix + "&c&l[" + bar + "&c&l] &f&l" + percent + "%"));
                
                // Sound effect with increasing pitch
                if ((int)progress % 10 == 0) {
                    float pitch = 0.5f + ((float) progress / maxProgress);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.4f, pitch);
                }
                
                // Subtle particles
                player.getWorld().spawnParticle(Particle.CRIT, arena.getCurrentPageLocation().clone().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.05);
            }
        }.runTaskTimer(SlenderMain.getInstance(), 0L, 2L);

        activeCollections.put(player.getUniqueId(), task);
    }

    private String getProgressBar(int current, int max, int totalBars) {
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);
        StringBuilder sb = new StringBuilder();
        sb.append("&f");
        for (int i = 0; i < progressBars; i++) {
            sb.append("■");
        }
        sb.append("&8");
        for (int i = 0; i < totalBars - progressBars; i++) {
            sb.append("■");
        }
        return sb.toString();
    }

    private void cancelCollection(UUID uuid) {
        BukkitTask task = activeCollections.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void pickupEvent(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        event.setCancelled(true);
        if(gamePlayer == null || !gamePlayer.isInArena())
            return;
        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null) return;
        if(arena.getPlayers().get(gamePlayer.getPlayer()) != Role.SURVIVOR)
            return;
            
        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack.getType() != org.bukkit.Material.PLAYER_HEAD && itemStack.getType() != Material.SKELETON_SKULL) {
            return;
        }
        
        org.bukkit.inventory.meta.ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(new NamespacedKey(SlenderMain.getInstance(), "page"), org.bukkit.persistence.PersistentDataType.BYTE)) {
            return;
        }

        event.getItem().remove();
        collectPage(player, gamePlayer, arena);
    }
    
    private void collectPage(Player player, GamePlayer gamePlayer, Arena arena) {
        arena.setCollectedPages(arena.getCollectedPages()+1);
        gamePlayer.setStatistic(Statistic.COLLECTED_PAGES, gamePlayer.getStatistic(Statistic.COLLECTED_PAGES)+1);
        SlenderMain.getInstance().getLevelManager().addExp(gamePlayer, 5);

        // PrayerSpeed perk: faster collection
        Perk perk = gamePlayer.getPerk(Role.SURVIVOR);
        if (perk instanceof PrayerSpeed) {
            player.sendActionBar(Component.text()
                    .append(Component.text("Prayer Speed ", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("- Collection boosted!", NamedTextColor.GRAY))
                    .build());
        }

        SlenderSurvivorPickupPageEvent slenderSurvivorPickupPageEvent = new SlenderSurvivorPickupPageEvent(gamePlayer, arena, arena.getCollectedPages());
        Bukkit.getPluginManager().callEvent(slenderSurvivorPickupPageEvent);

        int pagesToWin = Config.PAGES_TO_WIN.toInt();
        if(arena.getCollectedPages() >= pagesToWin) {
            arena.endGame(Role.SURVIVOR);
            return;
        }
        arena.spawnPage();
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gamePlayer != null && gamePlayer.isInArena()) {
            event.setCancelled(true);
        }
    }

}

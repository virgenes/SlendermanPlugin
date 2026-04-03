package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.SlenderDamageSurvivorEvent;
import me.dreamdevs.slender.api.events.SlenderKillSurvivorEvent;
import me.dreamdevs.slender.api.events.SlenderSurvivorPickupPageEvent;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.CustomItem;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameListeners implements Listener {

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
        if(attackerRole == Role.SPECTATOR || attackerRole == Role.NONE) {
            event.setCancelled(true);
            return;
        }

        if(attackerRole == Role.SURVIVOR && arena.getPlayers().get(entity) == Role.SURVIVOR) {
            event.setCancelled(true);
        }
        
        if (attackerRole == Role.SURVIVOR && arena.getPlayers().get(entity) == Role.SLENDER) {
            org.bukkit.inventory.ItemStack hand = damager.getInventory().getItemInMainHand();
            if (hand.getType() == org.bukkit.Material.WOODEN_SWORD && hand.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
                NamespacedKey hitsKey = new NamespacedKey(SlenderMain.getInstance(), "sword_hits_left");
                int hitsLeft = meta.getPersistentDataContainer().getOrDefault(hitsKey, PersistentDataType.INTEGER, 3);
                
                if (hitsLeft > 0) {
                    hitsLeft--;
                    
                    // Stun Slenderman
                    entity.addPotionEffect(new PotionEffect(me.dreamdevs.slender.compat.VersionCompat.getPotionType("SLOWNESS", "SLOW"), 80, 255, false, false));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1, false, false));
                    
                    damager.playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);
                    entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 20);
                    
                    // Trigger combat music for all arena players
                    Arena damagerArena = (Arena) SlenderMain.getInstance().getPlayerManager().getPlayer(damager).getArena();
                    if (damagerArena != null && damagerArena.getMusicManager() != null) {
                        damagerArena.getMusicManager().triggerCombat();
                    }
                    
                    if (hitsLeft <= 0) {
                        damager.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
                        damager.playSound(damager.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                        damager.sendMessage(ColourUtil.colorizeToComponent("&cYour sword has broken!"));
                    } else {
                        meta.getPersistentDataContainer().set(hitsKey, PersistentDataType.INTEGER, hitsLeft);
                        List<Component> lore = new ArrayList<>();
                        lore.add(ColourUtil.colorizeToComponent("&eUses remaining: " + hitsLeft));
                        meta.lore(lore);
                        hand.setItemMeta(meta);
                    }
                }
            }
            event.setCancelled(true); // Always cancel actual damage to Slenderman
        }

        if(arena.getSlenderMan().equals(attackerPlayer.getPlayer())) {
            SlenderDamageSurvivorEvent slenderManDamageSurvivorEvent = new SlenderDamageSurvivorEvent(attackerPlayer, victimPlayer, arena, event.getDamage());
            Bukkit.getPluginManager().callEvent(slenderManDamageSurvivorEvent);
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

        if(arena.getPlayers().get(gamePlayer.getPlayer()) == Role.SURVIVOR) {
            GamePlayer slender = SlenderMain.getInstance().getPlayerManager().getPlayer(arena.getSlenderMan());
            if (slender != null) {
                SlenderMain.getInstance().getLevelManager().addExp(slender, 5);
                SlenderKillSurvivorEvent slenderManKillSurvivorEvent = new SlenderKillSurvivorEvent(slender, gamePlayer, arena);
                Bukkit.getPluginManager().callEvent(slenderManKillSurvivorEvent);
                slender.setStatistic(Statistic.KILLED_SURVIVORS, slender.getStatistic(Statistic.KILLED_SURVIVORS)+1);
            }

            arena.getPlayers().put(gamePlayer.getPlayer(), Role.SPECTATOR);

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
            }

            event.getEntity().getLocation().getWorld().strikeLightningEffect(event.getEntity().getLocation());
            arena.sendMessage(Langauge.ARENA_KILLED_BY_SLENDER_MAN.toString().replace("%PLAYER%", gamePlayer.getPlayer().getName()));

            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                gamePlayer.getPlayer().spigot().respawn();
                gamePlayer.getPlayer().setGlowing(false);
                gamePlayer.getPlayer().sendMessage(ColourUtil.colorizeToComponent(Langauge.ARENA_SPECTATOR_MODE.toString()));
            }, 4L);

        }

        if (gamePlayer.getPlayer().getKiller() == null)
            return;

        if(arena.getPlayers().get(gamePlayer.getPlayer()) == Role.SLENDER) {
            arena.sendMessage(Langauge.ARENA_KILLED_BY_SURVIVOR.toString());
            GamePlayer killer = SlenderMain.getInstance().getPlayerManager().getPlayer(gamePlayer.getPlayer().getKiller());
            if (killer != null) {
                killer.setStatistic(Statistic.KILLED_SLENDERMEN, killer.getStatistic(Statistic.KILLED_SLENDERMEN)+1);
                SlenderMain.getInstance().getLevelManager().addExp(killer, 10);
            }
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> gamePlayer.getPlayer().spigot().respawn(), 4L);
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
            
        if (event.getItem().getItemStack().getType() != org.bukkit.Material.PAPER) {
            return;
        }

        event.getItem().remove();
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

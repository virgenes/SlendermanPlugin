package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.inventory.ItemMenuHolder;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.CustomItem;
import me.dreamdevs.slender.game.perks.*;
import me.dreamdevs.slender.menus.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInteractListener implements Listener {

    // Lantern uses tracker: player UUID -> remaining uses
    private static final Map<UUID, Integer> lanternUses = new ConcurrentHashMap<>();
    private static final int MAX_LANTERN_USES = 5;

    // Radar cooldown tracker: player UUID -> last use timestamp
    private static final Map<UUID, Long> radarCooldowns = new ConcurrentHashMap<>();
    private static final long RADAR_COOLDOWN_MS = 100_000L; // 100 seconds

    // Perk ability cooldown tracker
    private static final Map<UUID, Long> perkCooldowns = new ConcurrentHashMap<>();
    private static final long PERK_COOLDOWN_MS = 60_000L; // 60 seconds

    @EventHandler
    public void interactEvent(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getItemMeta() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack itemStack = event.getItem();
        ItemMeta meta = itemStack.getItemMeta();
        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : "";
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

        // === LOBBY ITEMS ===
        if (displayName.equals(CustomItem.ARENA_SELECTOR.getDisplayName()) && lore.equals(CustomItem.ARENA_SELECTOR.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new SelectArenaMenu().open(player);
            return;
        }

        if (displayName.equals(CustomItem.LEAVE.getDisplayName()) && lore.equals(CustomItem.LEAVE.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arena = (Arena) gamePlayer.getArena();
            SlenderMain.getInstance().getGameManager().leaveGame(gamePlayer.getPlayer(), arena);
            return;
        }

        if (displayName.equals(CustomItem.MY_PROFILE.getDisplayName()) && lore.equals(CustomItem.MY_PROFILE.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new MyProfileMenu(player).open(player);
            return;
        }

        if (displayName.equals(CustomItem.PLAY_AGAIN.getDisplayName()) && lore.equals(CustomItem.PLAY_AGAIN.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arena = (Arena) gamePlayer.getArena();
            Arena randomArena = SlenderMain.getInstance().getGameManager().getArenas()
                    .stream().filter(rArena -> (rArena.getArenaState() == ArenaState.WAITING
                            || rArena.getArenaState() == ArenaState.STARTING)
                            && !rArena.getPlayers().containsKey(player)).findFirst().orElse(null);
            if (randomArena == null) {
                player.sendMessage(Langauge.ARENA_NO_AVAILABLE_ARENAS.toString());
                return;
            }
            SlenderMain.getInstance().getGameManager().leaveGame(gamePlayer.getPlayer(), arena);
            SlenderMain.getInstance().getGameManager().joinGame(player, randomArena);
            return;
        }

        if (displayName.equals(CustomItem.SPECTATOR_SETTINGS.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new SpectatorSettingsMenu().open(player);
            return;
        }

        if (displayName.equals(CustomItem.PARTY_MENU.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new PartyMenu(gamePlayer).open(player);
            return;
        }

        if (displayName.equals(CustomItem.PERKS.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new PerkMenu().open(player);
            return;
        }

        if (displayName.equals(CustomItem.SPECTATOR_TELEPORTER.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            if (gamePlayer != null && gamePlayer.getArena() != null) {
                new TeleporterMenu((Arena) gamePlayer.getArena()).open(player);
            }
            return;
        }

        if (displayName.equals(CustomItem.SHOP.getDisplayName()) && lore.equals(CustomItem.SHOP.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            if (gamePlayer != null) {
                new ShopMenu(gamePlayer).open(player);
            }
            return;
        }

        // === GAMEPLAY ITEMS ===
        if (gamePlayer == null || !gamePlayer.isInArena()) return;
        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null) return;

        // === SURVIVOR LANTERN ===
        if (itemStack.getType() == Material.LANTERN && displayName.contains("Lantern")) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SURVIVOR) return;

            UUID uuid = player.getUniqueId();
            int uses = lanternUses.getOrDefault(uuid, MAX_LANTERN_USES);

            if (uses <= 0) {
                player.sendMessage(ColourUtil.colorize("&cYour lantern has no fuel left!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.3f);
                return;
            }

            uses--;
            lanternUses.put(uuid, uses);

            // Update item lore
            ItemMeta lMeta = itemStack.getItemMeta();
            lMeta.setLore(ColourUtil.colouredLore(Arrays.asList("&7Right-click to illuminate", "&7Uses: " + uses + "/" + MAX_LANTERN_USES)));
            itemStack.setItemMeta(lMeta);

            // Remove darkness temporarily (10 seconds of clear vision)
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 1));

            // Light up area with particles
            Location loc = player.getLocation();
            player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1.5, 0), 30, 2, 1, 2, 0.01);
            player.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, loc.clone().add(0, 1.5, 0), 20, 3, 1, 3, 0.02);
            player.playSound(player.getLocation(), Sound.BLOCK_LANTERN_PLACE, 1f, 1f);
            player.sendMessage(ColourUtil.colorize("&e&lLantern activated! &7(" + uses + " uses remaining)"));

            // Restore darkness after 10 seconds
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                if (player.isOnline() && arena.getPlayers().get(player) == Role.SURVIVOR) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
                }
            }, 200L);
            return;
        }

        // === SLENDERMAN RADAR ===
        if (itemStack.getType() == Material.CLOCK && displayName.contains("Radar")) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SLENDER) return;

            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            long lastUse = radarCooldowns.getOrDefault(uuid, 0L);

            if (now - lastUse < RADAR_COOLDOWN_MS) {
                long remaining = (RADAR_COOLDOWN_MS - (now - lastUse)) / 1000L;
                player.sendMessage(ColourUtil.colorize("&cRadar on cooldown! &7(" + remaining + "s remaining)"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.3f);
                return;
            }

            radarCooldowns.put(uuid, now);

            // Show all survivors on radar for 10 seconds
            player.sendMessage(ColourUtil.colorize("&c&lRADAR ACTIVE &7- Scanning for survivors..."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

            // Show particles at survivor locations for 10 seconds
            Bukkit.getScheduler().runTaskTimer(SlenderMain.getInstance(), () -> {
                for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
                    Player target = entry.getKey();
                    Role targetRole = entry.getValue();
                    if (targetRole != Role.SURVIVOR || !target.isOnline()) continue;

                    Location tLoc = target.getLocation();
                    // Visible to SlenderMan only
                    player.getWorld().spawnParticle(Particle.FLAME, tLoc.clone().add(0, 2, 0), 5, 0.5, 1, 0.5, 0);
                    player.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, tLoc.clone().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0);
                }
            }, 0L, 20L);

            // Stop radar after 10 seconds
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                player.sendMessage(ColourUtil.colorize("&cRadar scan complete."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.5f);
            }, 200L);
            return;
        }

        // === SURVIVOR PERK ABILITY ITEM ===
        if (itemStack.getType() == Material.BLAZE_POWDER && displayName.contains("Perk")) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SURVIVOR) return;

            Perk perk = gamePlayer.getPerk(Role.SURVIVOR);
            if (perk == null) {
                player.sendMessage(ColourUtil.colorize("&cNo perk equipped!"));
                return;
            }
            activatePerkAbility(player, gamePlayer, perk, Role.SURVIVOR);
            return;
        }

        // === SLENDERMAN PERK ABILITY ITEM ===
        if (itemStack.getType() == Material.BLAZE_ROD && displayName.contains("Perk")) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SLENDER) return;

            Perk perk = gamePlayer.getPerk(Role.SLENDER);
            if (perk == null) {
                player.sendMessage(ColourUtil.colorize("&cNo perk equipped!"));
                return;
            }
            activatePerkAbility(player, gamePlayer, perk, Role.SLENDER);
            return;
        }
    }

    private void activatePerkAbility(Player player, GamePlayer gamePlayer, Perk perk, Role role) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUse = perkCooldowns.getOrDefault(uuid, 0L);

        if (now - lastUse < PERK_COOLDOWN_MS) {
            long remaining = (PERK_COOLDOWN_MS - (now - lastUse)) / 1000L;
            player.sendMessage(ColourUtil.colorize("&cPerk on cooldown! &7(" + remaining + "s remaining)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.3f);
            return;
        }

        perkCooldowns.put(uuid, now);
        PerkInfo info = perk.getClass().getAnnotation(PerkInfo.class);
        player.sendMessage(ColourUtil.colorize("&6&l" + info.name() + " &7activated!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Execute perk-specific ability
        if (perk instanceof Runaway) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            player.sendMessage(ColourUtil.colorize("&eSpeed II for 5 seconds!"));
        } else if (perk instanceof BetterTogether) {
            player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.removePotionEffect(PotionEffectType.BLINDNESS);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0));
                    });
            player.sendMessage(ColourUtil.colorize("&aAll nearby allies can now see!"));
        } else if (perk instanceof Archaeologist) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 0));
            player.sendMessage(ColourUtil.colorize("&eRegeneration + Speed + Night Vision activated!"));
        } else if (perk instanceof Resilience) {
            // Passive perk - no active ability, just a message
            player.sendMessage(ColourUtil.colorize("&aResilience is always active - your sanity drains slower!"));
        } else if (perk instanceof Tracking) {
            // Show nearest page direction
            player.sendMessage(ColourUtil.colorize("&aTracking: Look for the particles pointing to the nearest page!"));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.6, 0), 20, 0.3, 0.3, 0.3, 0.05);
        } else if (perk instanceof Echo) {
            // Passive perk
            player.sendMessage(ColourUtil.colorize("&aEcho is always active - you can hear the SlenderMan from further away!"));
        } else if (perk instanceof Spirit) {
            // Passive perk (triggers on death)
            player.sendMessage(ColourUtil.colorize("&aSpirit is always active - if you die, your allies will be blessed!"));
        } else if (perk instanceof PrayerSpeed) {
            // Passive perk
            player.sendMessage(ColourUtil.colorize("&aPrayer Speed is always active - you collect pages faster!"));
        } else if (perk instanceof KillerInstinct) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            player.sendMessage(ColourUtil.colorize("&cSpeed boost activated!"));
        } else if (perk instanceof EndlessAgony) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
            player.sendMessage(ColourUtil.colorize("&cStrength boost activated!"));
        } else if (perk instanceof DarkAbyss) {
            // Apply blindness to all nearby survivors
            player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        Arena a = (Arena) gamePlayer.getArena();
                        if (a != null && a.getPlayers().get(p) == Role.SURVIVOR) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
                        }
                    });
            player.sendMessage(ColourUtil.colorize("&cDark Abyss: Nearby survivors are blinded!"));
        } else if (perk instanceof FromTheDark) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            player.sendMessage(ColourUtil.colorize("&cSpeed boost activated!"));
        } else if (perk instanceof PagesBelongings) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            player.sendMessage(ColourUtil.colorize("&cSpeed boost activated!"));
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
        if (event.getInventory().getHolder() instanceof ItemMenuHolder) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() == InventoryType.PLAYER) return;
        event.setResult(Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler
    public void inventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
        if (event.getInventory().getHolder() instanceof ItemMenuHolder) return;
        event.setResult(Event.Result.DENY);
        event.setCancelled(true);
    }

    // Clear lantern uses when player joins arena
    public static void clearLanternUses(Player player) {
        lanternUses.put(player.getUniqueId(), MAX_LANTERN_USES);
    }

    // Clear all trackers when player leaves arena
    public static void clearPlayerTrackers(Player player) {
        UUID uuid = player.getUniqueId();
        lanternUses.remove(uuid);
        radarCooldowns.remove(uuid);
        perkCooldowns.remove(uuid);
    }
}

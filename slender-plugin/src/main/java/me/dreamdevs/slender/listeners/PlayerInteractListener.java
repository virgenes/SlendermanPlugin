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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
        Component displayName = meta.displayName();
        List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

        // Plain text for name-based checks if needed
        String plainName = displayName != null ? PlainTextComponentSerializer.plainText().serialize(displayName) : "";

        // === LOBBY ITEMS ===
        if (Objects.equals(displayName, CustomItem.ARENA_SELECTOR.getDisplayName()) && Objects.equals(lore, CustomItem.ARENA_SELECTOR.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new SelectArenaMenu().open(player);
            return;
        }

        if (Objects.equals(displayName, CustomItem.LEAVE.getDisplayName()) && Objects.equals(lore, CustomItem.LEAVE.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arena = (Arena) gamePlayer.getArena();
            SlenderMain.getInstance().getGameManager().leaveGame(gamePlayer.getPlayer(), arena);
            return;
        }

        if (Objects.equals(displayName, CustomItem.MY_PROFILE.getDisplayName()) && Objects.equals(lore, CustomItem.MY_PROFILE.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new MyProfileMenu(player).open(player);
            return;
        }

        if (Objects.equals(displayName, CustomItem.PLAY_AGAIN.getDisplayName()) && Objects.equals(lore, CustomItem.PLAY_AGAIN.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arena = (Arena) gamePlayer.getArena();
            Arena randomArena = SlenderMain.getInstance().getGameManager().getArenas()
                    .stream().filter(rArena -> (rArena.getArenaState() == ArenaState.WAITING
                            || rArena.getArenaState() == ArenaState.STARTING)
                            && !rArena.getPlayers().containsKey(player)).findFirst().orElse(null);
            if (randomArena == null) {
                player.sendMessage(ColourUtil.colorizeToComponent(Langauge.ARENA_NO_AVAILABLE_ARENAS.toString()));
                return;
            }
            SlenderMain.getInstance().getGameManager().leaveGame(gamePlayer.getPlayer(), arena);
            SlenderMain.getInstance().getGameManager().joinGame(player, randomArena);
            return;
        }

        if (Objects.equals(displayName, CustomItem.SPECTATOR_SETTINGS.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new SpectatorSettingsMenu().open(player);
            return;
        }

        if (Objects.equals(displayName, CustomItem.PARTY_MENU.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new PartyMenu(gamePlayer).open(player);
            return;
        }

        if (Objects.equals(displayName, CustomItem.PERKS.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new PerkMenu().open(player);
            return;
        }

        if (Objects.equals(displayName, CustomItem.SPECTATOR_TELEPORTER.getDisplayName())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            if (gamePlayer != null && gamePlayer.getArena() != null) {
                new TeleporterMenu((Arena) gamePlayer.getArena()).open(player);
            }
            return;
        }

        if (Objects.equals(displayName, CustomItem.SHOP.getDisplayName()) && Objects.equals(lore, CustomItem.SHOP.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            if (gamePlayer != null) {
                new ShopMenu(gamePlayer).open(player);
            }
            return;
        }

        if (Objects.equals(displayName, CustomItem.FORCED_START.getDisplayName())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arenaObj = (Arena) gamePlayer.getArena();
            if (arenaObj.getArenaState() != ArenaState.WAITING && arenaObj.getArenaState() != ArenaState.STARTING) {
                player.sendMessage(Component.text("La partida ya ha comenzado.", NamedTextColor.RED));
                return;
            }
            if (arenaObj.getPlayers().size() < arenaObj.getMinPlayers()) {
                player.sendMessage(Component.text()
                        .append(Component.text("Se necesitan al menos ", NamedTextColor.RED))
                        .append(Component.text(arenaObj.getMinPlayers(), NamedTextColor.YELLOW))
                        .append(Component.text(" jugadores para iniciar.", NamedTextColor.RED))
                        .build());
                return;
            }
            if (arenaObj.getTimer() <= 5 && arenaObj.getArenaState() == ArenaState.STARTING) {
                player.sendMessage(Component.text("La partida ya está por iniciar.", NamedTextColor.RED));
                return;
            }
            
            arenaObj.setArenaState(ArenaState.STARTING);
            arenaObj.setTimer(5);
            arenaObj.sendMessage(Component.text()
                    .append(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" ha forzado el inicio de la partida. Empezamos en 5 segundos!", NamedTextColor.YELLOW))
                    .build());
            player.getInventory().remove(itemStack);
            return;
        }

        // === GAMEPLAY ITEMS ===
        if (gamePlayer == null || !gamePlayer.isInArena()) return;
        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null) return;

        // === SURVIVOR LANTERN ===
        if (itemStack.getType() == Material.LANTERN && plainName.contains("Lantern")) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SURVIVOR) return;

            UUID uuid = player.getUniqueId();
            int uses = lanternUses.getOrDefault(uuid, MAX_LANTERN_USES);

            if (uses <= 0) {
                player.sendMessage(Component.text("Your lantern has no fuel left!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.3f);
                return;
            }

            uses--;
            lanternUses.put(uuid, uses);

            // Update item lore
            ItemMeta lMeta = itemStack.getItemMeta();
            lMeta.lore(ColourUtil.colouredLoreToComponents(Arrays.asList("&7Right-click to illuminate", "&7Uses: " + uses + "/" + MAX_LANTERN_USES)));
            itemStack.setItemMeta(lMeta);

            // Remove darkness temporarily (10 seconds of clear vision)
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 1));

            // Light up area with particles
            Location loc = player.getLocation();
            player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1.5, 0), 30, 2, 1, 2, 0.01);
            player.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, loc.clone().add(0, 1.5, 0), 20, 3, 1, 3, 0.02);
            player.playSound(player.getLocation(), Sound.BLOCK_LANTERN_PLACE, 1f, 1f);
            player.sendMessage(Component.text()
                    .append(Component.text("Lantern activated! ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text("(" + uses + " uses remaining)", NamedTextColor.GRAY))
                    .build());

            // Restore darkness after 10 seconds
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                if (player.isOnline() && arena.getPlayers().get(player) == Role.SURVIVOR) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 4));
                }
            }, 200L);
            return;
        }

        // === SLENDERMAN RADAR ===
        if (itemStack.getType() == Material.CLOCK && plainName.contains("Radar")) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SLENDER) return;

            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            long lastUse = radarCooldowns.getOrDefault(uuid, 0L);

            if (now - lastUse < RADAR_COOLDOWN_MS) {
                long remaining = (RADAR_COOLDOWN_MS - (now - lastUse)) / 1000L;
                player.sendMessage(Component.text()
                        .append(Component.text("Radar on cooldown! ", NamedTextColor.RED))
                        .append(Component.text("(" + remaining + "s remaining)", NamedTextColor.GRAY))
                        .build());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.3f);
                return;
            }

            radarCooldowns.put(uuid, now);

            // Show all survivors on radar for 10 seconds
            player.sendMessage(Component.text()
                    .append(Component.text("RADAR ACTIVE ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("- Scanning for survivors...", NamedTextColor.GRAY))
                    .build());
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
                player.sendMessage(Component.text("Radar scan complete.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.5f);
            }, 200L);
            return;
        }

        // === PERK ABILITY ITEM ===
        if (lore.stream().anyMatch(c -> PlainTextComponentSerializer.plainText().serialize(c).contains("Your perk ability"))) {
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role == null || role == Role.NONE) return;

            Perk perk = gamePlayer.getPerk(role);
            if (perk == null) {
                player.sendMessage(Component.text("No perk equipped!", NamedTextColor.RED));
                return;
            }
            activatePerkAbility(player, gamePlayer, perk, role);
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

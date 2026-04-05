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
import me.dreamdevs.slender.game.FlashlightManager;
import me.dreamdevs.slender.game.perks.*;
import me.dreamdevs.slender.menus.*;
import me.dreamdevs.slender.menus.VoteModeMenu;
import me.dreamdevs.slender.menus.VoteDifficultyMenu;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInteractListener implements Listener {


    // Radar cooldown tracker: player UUID -> last use timestamp
    private static final Map<UUID, Long> radarCooldowns = new ConcurrentHashMap<>();
    private static final long RADAR_COOLDOWN_MS = 100_000L; // 100 seconds

    // Perk ability cooldown tracker
    private static final Map<UUID, Long> perkCooldowns = new ConcurrentHashMap<>();
    private static final long PERK_COOLDOWN_MS = 60_000L; // 60 seconds

    @EventHandler
    public void interactEvent(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();
        ItemMeta meta = itemStack != null ? itemStack.getItemMeta() : null;
        Component displayName = meta != null ? meta.displayName() : null;
        List<Component> lore = meta != null && meta.lore() != null ? meta.lore() : new ArrayList<>();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

        String plainName = displayName != null ? PlainTextComponentSerializer.plainText().serialize(displayName) : "";
        
        final Arena arena = (gamePlayer != null && gamePlayer.getArena() != null) 
                ? (Arena) gamePlayer.getArena() 
                : (player.hasMetadata("editing_arena") 
                    ? SlenderMain.getInstance().getGameManager().getArena(player.getMetadata("editing_arena").get(0).asString()) 
                    : null);

        // === LOBBY ITEMS ===
        if (itemStack != null && Objects.equals(displayName, CustomItem.ARENA_SELECTOR.getDisplayName()) && Objects.equals(lore, CustomItem.ARENA_SELECTOR.getLore())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new SelectArenaMenu().open(player);
            return;
        }

        if (itemStack != null && Objects.equals(displayName, CustomItem.FORCED_START.getDisplayName()) && Objects.equals(lore, CustomItem.FORCED_START.getLore())) {
            event.setCancelled(true);
            if (arena == null) return;
            if (arena.getPlayers().size() < arena.getMinPlayers()) {
                player.sendMessage(Langauge.ARENA_STOPPED_STARTING.toString());
                return;
            }
            arena.setTimer(10);
            player.sendMessage(Langauge.ARENA_FORCED_START_MSG.toString());
            player.getInventory().remove(itemStack);
            return;
        }

        // === PHASE 6: ARCHITECT TOOLS ===
        if (arena != null && (player.getGameMode() == GameMode.CREATIVE || arena.getArenaState() == ArenaState.WAITING || arena.getArenaState() == ArenaState.STARTING)) {
            
            // 1. GENERATOR TOOL
            if (itemStack != null && itemStack.getType() == Material.DAYLIGHT_DETECTOR && plainName.contains("Place Generator")) {
                event.setCancelled(true);
                if (event.getClickedBlock() == null) return;
                Location loc = event.getClickedBlock().getLocation();
                if (arena.getGeneratorLocations().contains(loc)) {
                    player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_ALREADY_EXISTS.toString());
                    return;
                }
                arena.getGeneratorLocations().add(loc);
                SlenderMain.getInstance().getGameManager().saveGame(arena);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_GEN_SET.toString().replace("%LOCATION%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                return;
            }
            
            // 2. MASTER KEY TOOL
            if (itemStack != null && itemStack.getType() == Material.TRIPWIRE_HOOK && plainName.contains("Place Master Key")) {
                event.setCancelled(true);
                if (event.getClickedBlock() == null) return;
                Location loc = event.getClickedBlock().getLocation();
                if (arena.getKeyLocations().contains(loc)) {
                    player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_ALREADY_EXISTS.toString());
                    return;
                }
                arena.getKeyLocations().add(loc);
                SlenderMain.getInstance().getGameManager().saveGame(arena);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_KEY_SET.toString().replace("%LOCATION%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                return;
            }
            
            // 3. ESCAPE POINT TOOL
            if (itemStack != null && itemStack.getType() == Material.IRON_DOOR && plainName.contains("Set Escape Point")) {
                event.setCancelled(true);
                if (event.getClickedBlock() == null) return;
                Location loc = event.getClickedBlock().getLocation();
                arena.setEscapeLocation(loc);
                SlenderMain.getInstance().getGameManager().saveGame(arena);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_ESCAPE_SET.toString().replace("%LOCATION%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                return;
            }

            // 4. SLENDER SPAWN TOOL
            if (itemStack != null && itemStack.getType() == Material.REDSTONE_BLOCK && plainName.contains("Set Slender Spawn")) {
                event.setCancelled(true);
                if (event.getClickedBlock() == null) return;
                Location loc = event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5);
                arena.setSlenderManSpawnLocation(loc);
                SlenderMain.getInstance().getGameManager().saveGame(arena);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_SLENDER_SET.toString().replace("%LOCATION%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                return;
            }
            
            // 5. SURVIVOR SPAWN TOOL
            if (itemStack != null && itemStack.getType() == Material.BEACON && plainName.contains("Add Survivor Spawn")) {
                event.setCancelled(true);
                if (event.getClickedBlock() == null) return;
                Location loc = event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5);
                if (arena.getSurvivorsLocations().contains(loc)) {
                     player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_ALREADY_EXISTS.toString());
                     return;
                }
                arena.getSurvivorsLocations().add(loc);
                SlenderMain.getInstance().getGameManager().saveGame(arena);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.8f);
                player.sendMessage(Langauge.ER_ARCHITECT_PREFIX.toString() + Langauge.ER_ARCHITECT_SURVIVOR_SET.toString().replace("%LOCATION%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                return;
            }
        }

        // === PHASE 6/7: SLENDERMAN TRAPS ===
        if (arena != null && arena.getArenaState() == ArenaState.RUNNING) {
            Role role = arena.getPlayers().get(player);
            if (role == Role.SLENDER && Objects.equals(displayName, CustomItem.SLENDER_TRAP.getDisplayName())) {
                event.setCancelled(true);
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    Location trapLoc = event.getClickedBlock().getLocation().add(0, 1, 0);
                    if (arena.getTrapLocations().size() >= 3) {
                        player.sendMessage(ColourUtil.colorize("&cYou can only place 3 traps at a time!"));
                        return;
                    }
                    arena.getTrapLocations().add(trapLoc);
                    itemStack.setAmount(itemStack.getAmount() - 1);
                    player.sendMessage(ColourUtil.colorize("&c&lTRAP PLACED! &7Awaiting victims..."));
                    player.playSound(player.getLocation(), Sound.BLOCK_SCAFFOLDING_PLACE, 1f, 0.5f);
                }
                return;
            }

            // === ESCAPE NOTE ===
            if (role == Role.SURVIVOR && Objects.equals(displayName, CustomItem.ESCAPE_NOTE.getDisplayName())) {
                event.setCancelled(true);
                String code = arena.getEscapeCode();
                player.sendMessage(ColourUtil.colorize("&f&lNOTE CONTENT: &7The code for the exit is &e&l" + (code != null ? code : "????")));
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                return;
            }
        }

        // === PHASE 6: ESCAPE ROOM GAMEPLAY ===
        if (arena != null && arena.getArenaType() == me.dreamdevs.slender.api.game.ArenaType.ESCAPE_ROOM && arena.getArenaState() == ArenaState.RUNNING) {
            Role role = arena.getPlayers().get(player);
            
            // Interaction with blocks (Generators, Escape Point)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                Location clickedLoc = event.getClickedBlock().getLocation();
                
                // 1. Generator Interaction
                for (Location genLoc : arena.getGeneratorLocations()) {
                    if (genLoc.equals(clickedLoc)) {
                        event.setCancelled(true);
                        if (role == Role.SURVIVOR) {
                            new me.dreamdevs.slender.menus.GeneratorMenu(arena, genLoc, player).open(player);
                        } else if (role == Role.SLENDER) {
                            handleGeneratorSabotage(player, arena, genLoc);
                        }
                        return;
                    }
                }
                
                // 2. Key Pickup Logic (If item is dropped or block represents key)
                for (Location keyLoc : arena.getKeyLocations()) {
                    if (keyLoc.equals(clickedLoc)) {
                        event.setCancelled(true);
                        if (role == Role.SURVIVOR) {
                            if (!player.getInventory().contains(CustomItem.ER_MASTER_KEY.toItemStack().getType())) {
                                player.getInventory().addItem(CustomItem.ER_MASTER_KEY.toItemStack());
                                player.sendMessage(Langauge.ER_MASTER_KEY_FOUND.toString());
                                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
                            }
                        }
                        return;
                    }
                }
                
                // 3. Escape Point Interaction
                if (arena.getEscapeLocation() != null && arena.getEscapeLocation().equals(clickedLoc)) {
                    event.setCancelled(true);
                    if (role == Role.SURVIVOR) {
                        if (player.getInventory().contains(CustomItem.ER_MASTER_KEY.toItemStack().getType())) {
                            if (arena.getGeneratorsRepaired() >= 3) {
                                new KeypadMenu(arena).open(player);
                            } else {
                                player.sendMessage(ColourUtil.colorize("&cYou need to repair at least 3 generators first!"));
                            }
                        } else {
                            player.sendMessage(ColourUtil.colorize("&cYou need the Master Key to open the escape route!"));
                        }
                    }
                    return;
                }
            }
        }

        if (Objects.equals(displayName, CustomItem.LEAVE.getDisplayName()) && Objects.equals(lore, CustomItem.LEAVE.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arenaObj = (Arena) gamePlayer.getArena();
            SlenderMain.getInstance().getGameManager().leaveGame(gamePlayer.getPlayer(), arenaObj);
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
            if (arena == null) return;
            Arena randomArena = SlenderMain.getInstance().getGameManager().getArenas()
                    .stream().filter(rArena -> (rArena.getArenaState() == ArenaState.WAITING
                            || rArena.getArenaState() == ArenaState.STARTING)
                            && !rArena.getPlayers().containsKey(player)).findFirst().orElse(null);
            if (randomArena == null) {
                player.sendMessage(ColourUtil.colorizeToComponent(Langauge.ARENA_NO_AVAILABLE_ARENAS.toString()));
                return;
            }
            if (gamePlayer == null || player == null || randomArena == null) return;
            SlenderMain.getInstance().getGameManager().leaveGame(player, arena);
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

        if (Objects.equals(displayName, CustomItem.VOTE_MODE.getDisplayName()) && Objects.equals(lore, CustomItem.VOTE_MODE.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arenaStr = (Arena) gamePlayer.getArena();
            if (arenaStr.getArenaState() != ArenaState.WAITING && arenaStr.getArenaState() != ArenaState.STARTING) return;
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new VoteModeMenu(arenaStr).open(player);
            return;
        }

        if (Objects.equals(displayName, CustomItem.VOTE_DIFFICULTY.getDisplayName()) && Objects.equals(lore, CustomItem.VOTE_DIFFICULTY.getLore())) {
            event.setCancelled(true);
            if (gamePlayer == null || gamePlayer.getArena() == null) return;
            Arena arenaStr = (Arena) gamePlayer.getArena();
            if (arenaStr.getArenaState() != ArenaState.WAITING && arenaStr.getArenaState() != ArenaState.STARTING) return;
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, (float) Math.random());
            new VoteDifficultyMenu(arenaStr).open(player);
            return;
        }

        // === GAMEPLAY ITEMS ===
        if (arena == null) return;

        // === SURVIVOR FLASHLIGHT ===
        // The Flashlight custom item uses the PersistentDataContainer via the battery key, 
        // so we check if FlashlightManager should handle it.
        if (itemStack.getType() != Material.AIR && itemStack.hasItemMeta() &&
            itemStack.getItemMeta().getPersistentDataContainer().has(FlashlightManager.BATTERY_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            
            event.setCancelled(true);
            Role role = arena.getPlayers().get(player);
            if (role != Role.SURVIVOR) return;

            // Handle Overload Mode (Sneaking + Right Click)
            if (player.isSneaking() && event.getAction().name().contains("RIGHT")) {
                // To be implemented or handled inside FlashlightManager
                arena.getFlashlightManager().toggle(player);
                return;
            }

            // Normal Toggle
            if (event.getAction().name().contains("RIGHT")) {
                arena.getFlashlightManager().toggle(player);
            }
            return;
        }
        
        // === FLASH ESCAPE BUCKET ===
        if (itemStack.getType() == Material.WATER_BUCKET && itemStack.hasItemMeta() && 
            itemStack.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(SlenderMain.getInstance(), "flash_escape"), org.bukkit.persistence.PersistentDataType.BYTE)) {
            event.setCancelled(true);
            
            Role bRole = arena.getPlayers().get(player);
            if (bRole != Role.SURVIVOR) return;

            Player arenaSlender = arena.getSlenderMan();
            if (arenaSlender != null && arenaSlender.isOnline()) {
                if (player.getWorld().equals(arenaSlender.getWorld()) && player.getLocation().distance(arenaSlender.getLocation()) <= 10.0) {
                    // Consume Bucket
                    player.getInventory().remove(itemStack);
                    
                    // Teleport and Sound
                    arenaSlender.teleport(arena.getSlenderManSpawnLocation(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    arenaSlender.playSound(arenaSlender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
                    arenaSlender.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 1));
                    
                    player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&bYou banished the Slenderman!"));
                    arenaSlender.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&cYou were banished by a Flash Escape!"));
                } else {
                    player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&cSlenderman is not close enough!"));
                }
            }
            return;
        }

        // === SLENDERMAN RADAR ===
        if (itemStack.getType() == Material.CLOCK && plainName.contains("Radar")) {
            event.setCancelled(true);
            Role rRole = arena.getPlayers().get(player);
            if (rRole != Role.SLENDER) return;

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
            Role pRole = arena.getPlayers().get(player);
            if (pRole == null || pRole == Role.NONE) return;

            // Phase 4: HARD Mode Perk Block
            if (arena.getCurrentDifficulty() == me.dreamdevs.slender.api.game.Difficulty.HARD) {
                player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&c&lNightmare! &7Perks are disabled in Hard Difficulty."));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
                return;
            }

            if (gamePlayer == null || arena == null) return;
            Perk perk = gamePlayer.getPerk(pRole);
            if (perk == null) {
                player.sendMessage(Component.text("No perk equipped!", NamedTextColor.RED));
                return;
            }
            activatePerkAbility(player, gamePlayer, perk, pRole, arena);
            return;
        }
    }

    private void activatePerkAbility(Player player, GamePlayer gamePlayer, Perk perk, Role role, Arena arena) {
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
                        if (arena != null && arena.getPlayers().get(p) == Role.SURVIVOR) {
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

    // === PHASE 6: ESCAPE ROOM HANDLERS ===

    private static final Map<UUID, BukkitTask> repairTasks = new HashMap<>();

    public static void startRepair(Player player, Arena arena, Location loc) {
        if (repairTasks.containsKey(player.getUniqueId())) return;
        
        Double progress = arena.getGeneratorProgress().getOrDefault(loc, 0.0);
        if (progress >= 100.0) {
            player.sendMessage(Langauge.ER_GENERATOR_ALREADY_REPAIRED.toString());
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            private double currentProgress = progress;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    repairTasks.remove(player.getUniqueId());
                    return;
                }
                
                // If player moves too far (more than 3 blocks)
                if (player.getLocation().distance(loc) > 4.0) {
                    player.sendMessage(Langauge.ER_GENERATOR_TOO_FAR.toString());
                    cancel();
                    repairTasks.remove(player.getUniqueId());
                    return;
                }

                currentProgress += 10.0; // 10% per second
                arena.getGeneratorProgress().put(loc, currentProgress);
                
                // Visuals
                String bar = "|".repeat((int)currentProgress/5);
                player.sendActionBar(Langauge.ER_GENERATOR_REPAIR_ACTIONBAR.toString()
                    .replace("%PROGRESS%", String.valueOf((int)currentProgress))
                    .replace("%BAR%", bar));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);
                loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0.5, 1, 0.5), 5, 0.2, 0.2, 0.2, 0.05);

                if (currentProgress >= 100.0) {
                    arena.getGeneratorProgress().put(loc, 100.0);
                    arena.setGeneratorsRepaired(arena.getGeneratorsRepaired() + 1);
                    
                    arena.sendMessage(Langauge.ER_GENERATOR_REPAIR_SUCCESS.toString()
                        .replace("%CURRENT%", String.valueOf(arena.getGeneratorsRepaired()))
                        .replace("%TOTAL%", String.valueOf(arena.getGeneratorLocations().size())));
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    cancel();
                    repairTasks.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(me.dreamdevs.slender.SlenderMain.getInstance(), 0L, 20L);

        repairTasks.put(player.getUniqueId(), task);
        player.sendMessage(Langauge.ER_GENERATOR_REPAIR_START.toString());
    }

    private void handleGeneratorRepair(Player player, Arena arena, Location loc) {
        startRepair(player, arena, loc);
    }

    private void handleGeneratorSabotage(Player player, Arena arena, Location loc) {
        Double progress = arena.getGeneratorProgress().getOrDefault(loc, 0.0);
        if (progress <= 0) return;

        double reduction = 20.0; // Sabotage power
        double newProgress = Math.max(0, progress - reduction);
        arena.getGeneratorProgress().put(loc, newProgress);

        if (progress >= 100.0 && newProgress < 100.0) {
            arena.setGeneratorsRepaired(arena.getGeneratorsRepaired() - 1);
        }

        player.sendMessage(ColourUtil.colorize("&c&lGENERATOR SABOTAGED! &fProgress reduced."));
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
    }

    // Clear all trackers when player leaves arena
    public static void clearPlayerTrackers(Player player) {
        UUID uuid = player.getUniqueId();
        radarCooldowns.remove(uuid);
        perkCooldowns.remove(uuid);
    }
}

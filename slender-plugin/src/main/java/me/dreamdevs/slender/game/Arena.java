package me.dreamdevs.slender.game;

import lombok.Getter;
import lombok.Setter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.*;
import me.dreamdevs.slender.api.events.SlenderGameEndEvent;
import me.dreamdevs.slender.api.events.SlenderGameStartEvent;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.IArena;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.GameMode;
import me.dreamdevs.slender.api.game.Difficulty;
import me.dreamdevs.slender.api.game.ArenaType;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import me.dreamdevs.slender.game.ScareTriggerManager;

import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;

@Getter @Setter
public class Arena extends BukkitRunnable implements IArena {

    private final String id;
    private int minPlayers;
    private int maxPlayers;
    private int gameTime;
    private int timer;

    // Phase 1: New Setup Fields
    private ArenaType arenaType = ArenaType.STANDARD;
    private final List<GameMode> allowedModes = new ArrayList<>(Arrays.asList(GameMode.CLASSIC, GameMode.INFECTION));
    private GameMode activeMode = GameMode.CLASSIC;
    private Difficulty currentDifficulty = Difficulty.EASY;
    
    // Phase 3: Voting Maps
    private final Map<UUID, GameMode> modeVotes = new ConcurrentHashMap<>();
    private final Map<UUID, Difficulty> difficultyVotes = new ConcurrentHashMap<>();

    private Location slenderManSpawnLocation;
    private List<Location> survivorsLocations;
    private List<Location> pagesLocations;
    
    // Phase 6: Escape Room Locations
    private List<Location> generatorLocations = new ArrayList<>();
    private List<Location> keyLocations = new ArrayList<>();
    private Location escapeLocation;

    // Phase 6 Status
    private int generatorsRepaired = 0;
    private int keysFound = 0;
    private final Map<Location, Double> generatorProgress = new HashMap<>(); // Progress 0-100%
    private String escapeCode;
    private final List<Location> trapLocations = new ArrayList<>();
    private final Map<UUID, Long> trapCooldowns = new HashMap<>();

    private ArenaState arenaState;
    private Map<Player, Role> players;
    private Player slenderMan;
    private final Map<UUID, Role> persistentRoles = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Long> disconnectTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCombatHit = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<UUID> markedCheaters = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private BossBar bossBar;

    private int collectedPages;
    private Location currentPageLocation;
    private ArmorStand pageHologram;

    private Scoreboard scoreboard;
    private Objective objective;

    private File file;

    private BukkitTask radiusTask;
    private TerrorRadiusManager terrorRadius;
    private SanityManager sanityManager;
    private StealthManager stealthManager;
    private AmbientSoundManager ambientSoundManager;
    private FlashlightManager flashlightManager;
    private MusicManager musicManager;
    private ScareTriggerManager scareTriggerManager;
    private RevivalManager revivalManager;

    public Arena(String id) {
        this.id = id;
        this.arenaState = ArenaState.WAITING;
        this.players = new ConcurrentHashMap<>();
        this.survivorsLocations = new ArrayList<>();
        this.pagesLocations = new ArrayList<>();
        this.bossBar = Bukkit.createBossBar(Langauge.ARENA_BOSS_BAR_WAITING_TITLE.toString(), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY);
        this.slenderMan = null;

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.scoreboard.registerNewObjective(id, Criteria.DUMMY, ColourUtil.colorizeToComponent(id));

        this.scoreboard.registerNewTeam("survivors");
        this.scoreboard.registerNewTeam("slenderman");

        this.scoreboard.getTeam("survivors").setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        this.scoreboard.getTeam("slenderman").setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    public void startGame() {
        runTaskTimer(SlenderMain.getInstance(), 20L, 20L);
    }

    @Override
    public void run() {
        switch (arenaState) {
            case WAITING:
                sendTitleToAllPlayers("", "Waiting for players", 0, 25, 25);
                break;
            case STARTING:
                sendTitleToAllPlayers(Langauge.EMPTY.toString(), Langauge.ARENA_STARTING_SUBTITLE.toString().replace("%TIME%", String.valueOf(timer)), 0, 25, 25);
                if (timer == 0) {
                    start();
                    break;
                }
                timer--;
                break;
            case RUNNING:
                this.bossBar.setTitle(Langauge.ARENA_BOSS_BAR_RUNNING_TITLE.toString().replace("%TIME%", String.valueOf(timer)));
                sendActionBar(Langauge.ARENA_COLLECTED_PAGES.toString().replace("%CURRENT%", Integer.toString(collectedPages)));
                
                if (timer == 100) { // Distribute Water Bucket flash escape!
                    org.bukkit.inventory.ItemStack bucket = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WATER_BUCKET);
                    org.bukkit.inventory.meta.ItemMeta bMeta = bucket.getItemMeta();
                    if (bMeta != null) {
                        bMeta.displayName(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&bFlash Escape"));
                        bMeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(SlenderMain.getInstance(), "flash_escape"), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
                        java.util.List<net.kyori.adventure.text.Component> bLore = new java.util.ArrayList<>();
                        bLore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&7Right-click near Slenderman"));
                        bLore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&7to instantly banish him."));
                        bMeta.lore(bLore);
                        bucket.setItemMeta(bMeta);
                    }
                    
                    for (java.util.Map.Entry<Player, Role> entry : players.entrySet()) {
                        if (entry.getValue() == Role.SURVIVOR && entry.getKey().isOnline()) {
                            entry.getKey().getInventory().addItem(bucket);
                            entry.getKey().playSound(entry.getKey().getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                            entry.getKey().sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&b&lSupply Drop! &7You received a Flash Escape bucket!"));
                        }
                    }
                }
                
                if(timer == 0) {
                    endGame(Role.SLENDER);
                    return;
                }

                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : disconnectTimes.entrySet()) {
                    if (now - entry.getValue() > 30000) {
                        UUID uuid = entry.getKey();
                        Role role = persistentRoles.get(uuid);
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        
                        if (role == Role.SLENDER) {
                            sendMessage("&cSlenderman failed to reconnect in time! Survivors win.");
                            endGame(Role.SURVIVOR);
                            return;
                        } else if (role == Role.SURVIVOR || role == Role.DOWNED) {
                            disconnectTimes.remove(uuid);
                            sendMessage("&e" + name + " failed to reconnect and has been removed from the game.");
                            checkSurvivorsRemaining();
                        } else {
                            disconnectTimes.remove(uuid);
                        }
                    }
                }

                timer--;
                break;
            case ENDING:
                this.bossBar.setTitle(Langauge.ARENA_BOSS_BAR_ENDING_TITLE.toString().replace("%TIME%", String.valueOf(timer)));
                if(timer == 0) {
                    restart();
                    return;
                }
                timer--;
                break;
            case RESTARTING:
                break;
        }
    }

    public void start() {
        if (arenaType == ArenaType.STANDARD) {
            Map<GameMode, Integer> mCounts = new HashMap<>();
            for (GameMode mode : modeVotes.values()) {
                mCounts.put(mode, mCounts.getOrDefault(mode, 0) + 1);
            }
            GameMode wonMode = GameMode.CLASSIC;
            int maxModeVotes = -1;
            for (Map.Entry<GameMode, Integer> entry : mCounts.entrySet()) {
                if (entry.getValue() > maxModeVotes) {
                    maxModeVotes = entry.getValue();
                    wonMode = entry.getKey();
                }
            }
            this.activeMode = wonMode;

            Map<Difficulty, Integer> dCounts = new HashMap<>();
            for (Difficulty diff : difficultyVotes.values()) {
                dCounts.put(diff, dCounts.getOrDefault(diff, 0) + 1);
            }
            Difficulty wonDiff = Difficulty.EASY;
            int maxDiffVotes = -1;
            for (Map.Entry<Difficulty, Integer> entry : dCounts.entrySet()) {
                if (entry.getValue() > maxDiffVotes) {
                    maxDiffVotes = entry.getValue();
                    wonDiff = entry.getKey();
                }
            }
            this.currentDifficulty = wonDiff;

            sendMessage("&a&lVOTING RESULTS:");
            sendMessage("&e>> Mode: &b" + wonMode.name());
            sendMessage("&e>> Difficulty: &c" + wonDiff.name());
        }

        this.bossBar.setTitle(ColourUtil.colorize(Langauge.ARENA_BOSS_BAR_RUNNING_TITLE.toString().replace("%TIME%", String.valueOf(timer))));
        sendTitleToAllPlayers(Langauge.ARENA_TITLE.toString(), Langauge.ARENA_STARTED_SUBTITLE.toString(), 10, 30, 10);

        sendPlayersToGame();
        setArenaState(ArenaState.RUNNING);
        setTimer(gameTime);
        spawnPage();

        if (this.arenaType == ArenaType.ESCAPE_ROOM) {
            if (this.escapeCode == null || this.escapeCode.isEmpty()) {
                this.escapeCode = String.format("%04d", new java.util.Random().nextInt(10000));
            }
            spawnEscapeRoomObjectives();
        }

        if(Config.USE_TERROR_RADIUS.toBoolean()) {
            this.terrorRadius = new TerrorRadiusManager();
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                if (this.terrorRadius != null) this.terrorRadius.start(this);
            }, 20L);
        }

        this.sanityManager = new SanityManager();
        this.stealthManager = new StealthManager();
        this.ambientSoundManager = new AmbientSoundManager();
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
            if (this.sanityManager != null) this.sanityManager.start(this);
            if (this.stealthManager != null) {
                this.stealthManager.start(this);
                Bukkit.getPluginManager().registerEvents(this.stealthManager, SlenderMain.getInstance());
            }
            if (this.ambientSoundManager != null) this.ambientSoundManager.start(this);
            
            this.flashlightManager = new FlashlightManager(this);
            this.flashlightManager.start();
            
            this.musicManager = new MusicManager(this);
            this.musicManager.start();

            if (this.arenaType == ArenaType.ESCAPE_ROOM) {
                this.scareTriggerManager = new ScareTriggerManager(this);
                this.scareTriggerManager.runTaskTimer(SlenderMain.getInstance(), 0L, 20L);
            }
        }, 20L);
        
        if (Config.REVIVAL_ENABLED.toBoolean()) {
            this.revivalManager = new RevivalManager(this);
        }
        
        if (currentDifficulty != Difficulty.HARD) {
            me.dreamdevs.slender.game.perks.Tracking tracking = SlenderMain.getInstance().getPerkManager().getPerksByRole(Role.SURVIVOR)
                    .stream().filter(p -> p instanceof me.dreamdevs.slender.game.perks.Tracking)
                    .map(p -> (me.dreamdevs.slender.game.perks.Tracking) p).findFirst().orElse(null);
            if (tracking != null) {
                players.keySet().stream()
                        .filter(p -> {
                            me.dreamdevs.slender.database.data.GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(p);
                            return gp != null && gp.getPerk(Role.SURVIVOR) instanceof me.dreamdevs.slender.game.perks.Tracking;
                        })
                        .forEach(p -> {
                            Location pLoc = p.getLocation();
                            Location nearest = null;
                            double minDist = Double.MAX_VALUE;

                            for (Location pageLoc : pagesLocations) {
                                double d = pLoc.distance(pageLoc);
                                if (d < minDist) {
                                    minDist = d;
                                    nearest = pageLoc;
                                }
                            }

                            if (nearest != null) {
                                Vector dir = nearest.toVector().subtract(pLoc.toVector()).normalize().multiply(0.5);
                                p.spawnParticle(Particle.HAPPY_VILLAGER, pLoc.clone().add(0, 1.5, 0).add(dir), 5, 0.1, 0.1, 0.1, 0.02);
                            }
                        });
            }
        }
        
        SlenderGameStartEvent slenderGameStartEvent = new SlenderGameStartEvent(this);
        Bukkit.getPluginManager().callEvent(slenderGameStartEvent);
    }

    private void sendPlayersToGame() {
        List<Player> tempList = new ArrayList<>(players.keySet());
        int size = tempList.size();
        int random = Util.getRandomNumber(size);
        this.slenderMan = tempList.get(random);
        tempList.remove(slenderMan);
        tempList.forEach(player -> players.put(player, Role.SURVIVOR));
        players.put(slenderMan, Role.SLENDER);
        
        persistentRoles.clear();
        players.forEach((p, r) -> persistentRoles.put(p.getUniqueId(), r));
        disconnectTimes.clear();

        final Player finalSlenderMan = this.slenderMan;
        this.scoreboard.getTeam("slenderman").addPlayer(finalSlenderMan);
        finalSlenderMan.setGameMode(org.bukkit.GameMode.ADVENTURE);
        finalSlenderMan.getInventory().clear();
        finalSlenderMan.getInventory().setItem(0, CustomItem.SLENDERMAN_WEAPON.toItemStack());
        finalSlenderMan.getInventory().setItem(1, CustomItem.SLENDERMAN_RADAR.toItemStack());
        GamePlayer gameSlenderMan = SlenderMain.getInstance().getPlayerManager().getPlayer(finalSlenderMan);
        
        // Perk ability item
        Perk slenderPerk = gameSlenderMan.getPerk(Role.SLENDER);
        if (slenderPerk != null) {
            PerkInfo sInfo = slenderPerk.getClass().getAnnotation(PerkInfo.class);
            ItemStack perkItem = new ItemStack(sInfo.icon());
            ItemMeta sMeta = perkItem.getItemMeta();
            sMeta.displayName(ColourUtil.colorizeToComponent("&4&l" + sInfo.name()));
            sMeta.lore(ColourUtil.colouredLoreToComponents(Arrays.asList("&7Right-click to activate", "&7Your perk ability")));
            sMeta.setUnbreakable(true);
            perkItem.setItemMeta(sMeta);
            finalSlenderMan.getInventory().setItem(2, perkItem);
        }

        // Darkness effect for all survivors - total blindness
        tempList.forEach(player -> {
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            player.setFallDistance(0);
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            player.teleport(survivorsLocations.get(Util.getRandomNumber(survivorsLocations.size())), PlayerTeleportEvent.TeleportCause.PLUGIN);
            this.scoreboard.getTeam("survivors").addPlayer(player);
            // Total darkness: darkness effect (Level 1)
            me.dreamdevs.slender.compat.VersionCompat.applyDarkness(player, Integer.MAX_VALUE, 0);
            player.getInventory().clear();
            
            ItemStack weapon = CustomItem.SURVIVOR_WEAPON.toItemStack();
            ItemMeta wMeta = weapon.getItemMeta();
            if (wMeta != null) {
                org.bukkit.NamespacedKey hitsKey = new org.bukkit.NamespacedKey(me.dreamdevs.slender.SlenderMain.getInstance(), "sword_hits_left");
                wMeta.getPersistentDataContainer().set(hitsKey, org.bukkit.persistence.PersistentDataType.INTEGER, 3);
                List<net.kyori.adventure.text.Component> wLore = wMeta.lore();
                if (wLore == null) wLore = new ArrayList<>();
                wLore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&eUses remaining: 3"));
                wMeta.lore(wLore);
                weapon.setItemMeta(wMeta);
            }
            player.getInventory().setItem(0, weapon);
            
            if (this.flashlightManager == null) {
                this.flashlightManager = new FlashlightManager(this);
            }
            this.flashlightManager.giveFlashlight(player, 1);
            
            // Perk ability item (NOT auto-activated, must be clicked)
            GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
            Perk survivorPerk = gamePlayer.getPerk(Role.SURVIVOR);
            if (survivorPerk != null) {
                PerkInfo pInfo = survivorPerk.getClass().getAnnotation(PerkInfo.class);
                ItemStack perkItem = new ItemStack(pInfo.icon());
                ItemMeta pMeta = perkItem.getItemMeta();
                pMeta.displayName(ColourUtil.colorizeToComponent("&6&l" + pInfo.name()));
                pMeta.lore(ColourUtil.colouredLoreToComponents(Arrays.asList("&7Right-click to activate", "&7Your perk ability")));
                pMeta.setUnbreakable(true);
                perkItem.setItemMeta(pMeta);
                player.getInventory().setItem(2, perkItem);
            }
            
            // Give survivor map
            ItemStack mapItem = CustomItem.SURVIVOR_MAP.toItemStack();
            org.bukkit.inventory.meta.MapMeta mapMeta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
            if (mapMeta != null) {
                org.bukkit.map.MapView view = org.bukkit.Bukkit.createMap(player.getWorld());
                view.getRenderers().forEach(view::removeRenderer);
                view.addRenderer(new SurvivorMapRenderer(this));
                mapMeta.setMapView(view);
                mapItem.setItemMeta(mapMeta);
            }
            player.getInventory().setItem(3, mapItem);
        });

        finalSlenderMan.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        finalSlenderMan.setFallDistance(0);
        finalSlenderMan.teleport(slenderManSpawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        finalSlenderMan.getAttribute(me.dreamdevs.slender.utils.AttributeUtils.getMaxHealth()).setBaseValue(Config.SLENDERMAN_HEALTH.toInt());
        finalSlenderMan.setHealth(Config.SLENDERMAN_HEALTH.toInt());
        finalSlenderMan.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        if (Config.USE_DISGUISE.toBoolean()) {
            final SlenderDisguise skin = gameSlenderMan.getEquippedSkin();
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () ->
                    DisguiseManager.disguise(finalSlenderMan, skin), 10L);
        }

    }

    private void sendTitleToAllPlayers(String title, String subTitle, int fadeIn, int stayIn, int fadeOut) {
        if (players.isEmpty()) return;
        Title adventureTitle = Title.title(
                ColourUtil.colorizeToComponent(title),
                ColourUtil.colorizeToComponent(subTitle),
                Title.Times.times(Ticks.duration(fadeIn), Ticks.duration(stayIn), Ticks.duration(fadeOut))
        );
        players.keySet().forEach(player -> player.showTitle(adventureTitle));
    }

    private void sendActionBar(String message) {
        if (players.isEmpty()) return;
        players.keySet().forEach(player -> player.sendActionBar(ColourUtil.colorizeToComponent(message)));
    }

    public void sendMessage(String message) {
        if (players.isEmpty()) return;
        players.keySet().forEach(player -> player.sendMessage(ColourUtil.colorizeToComponent(message)));
    }

    public void sendMessage(net.kyori.adventure.text.Component component) {
        if (players.isEmpty()) return;
        players.keySet().forEach(player -> player.sendMessage(component));
    }

    public boolean isRunning() {
        return arenaState == ArenaState.RUNNING;
    }

    public int getSurvivorsAmount() {
        return (int) players.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.SURVIVOR || entry.getValue() == Role.DOWNED)
                .count();
    }

    public List<Player> getSurvivors() {
        return players.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.SURVIVOR || entry.getValue() == Role.DOWNED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<Player> getDownedPlayers() {
        return players.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.DOWNED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public RevivalManager getRevivalManager() {
        return revivalManager;
    }

    public void restart() {
        setArenaState(ArenaState.RESTARTING);
        cleanupBossBar();
        // Remove entities/items
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream()
                .filter(entity -> entity instanceof Item)
                .forEach(Entity::remove));
        
        if (this.flashlightManager != null) {
            this.flashlightManager.stop();
            this.flashlightManager = null;
        }
        if (this.musicManager != null) {
            this.musicManager.stop();
            this.musicManager = null;
        }
        if (this.scareTriggerManager != null) {
            this.scareTriggerManager.cancel();
            this.scareTriggerManager = null;
        }
        if (this.sanityManager != null) {
            this.sanityManager.stop();
            this.sanityManager = null;
        }
        if (this.terrorRadius != null) {
            this.terrorRadius.stop();
            this.terrorRadius = null;
        }
        if (this.stealthManager != null) {
            this.stealthManager.stop();
            this.stealthManager = null;
        }
        if (this.ambientSoundManager != null) {
            this.ambientSoundManager.stop();
            this.ambientSoundManager = null;
        }
        if (this.revivalManager != null) {
            this.revivalManager.stop();
            this.revivalManager = null;
        }
        
        removeCurrentPage();

        // Reset players
        players.keySet().forEach(player -> {
            SlenderMain.getInstance().getPlayerManager().sendToLobby(player);
        });

        players.clear();
        persistentRoles.clear();
        disconnectTimes.clear();
        this.slenderMan = null;
        this.collectedPages = 0;
        cleanupBossBar();
        this.generatorsRepaired = 0;
        this.keysFound = 0;
        this.generatorProgress.clear();
        this.trapLocations.clear();
        this.trapCooldowns.clear();
        this.escapeCode = null;
        cleanupEscapeRoomObjectives();

        setArenaState(ArenaState.WAITING);
        this.lastCombatHit.clear();
        this.markedCheaters.clear();
    }

    public void cleanupBossBar() {
        if (this.bossBar != null) {
            this.bossBar.removeAll();
        }
    }

    public void handleDisconnect(Player player) {
        if (!players.containsKey(player)) return;
        UUID uuid = player.getUniqueId();
        
        // Anti-combat-log check
        long lastHit = lastCombatHit.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastHit < 10000) { // 10 seconds timeout
            markedCheaters.add(uuid);
            persistentRoles.put(uuid, Role.SPECTATOR); // Forced to spectator on return
            sendMessage(ColourUtil.colorize("&c&lCHEATER DETECTED! &f" + player.getName() + " disconnected during combat."));
        } else {
            sendMessage("&c" + player.getName() + " has disconnected! They have 30 seconds to reconnect.");
        }
        
        disconnectTimes.put(uuid, System.currentTimeMillis());
        players.remove(player);
        
        // Immediate win check if last survivor combat-logs
        checkSurvivorsRemaining();
    }

    public void handleReconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (!disconnectTimes.containsKey(uuid)) return;
        
        Role role;
        if (markedCheaters.contains(uuid)) {
            role = Role.SPECTATOR;
            players.put(player, role);
            player.sendMessage(ColourUtil.colorize("&c&lWARNING! &7You were marked as a combat-logger. You are now a spectator."));
        } else {
            role = persistentRoles.get(uuid);
            players.put(player, role);
        }
        disconnectTimes.remove(uuid);
        player.setScoreboard(this.scoreboard);
        this.bossBar.addPlayer(player);

        if (role == Role.SLENDER) {
            this.slenderMan = player;
            player.teleport(slenderManSpawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.getInventory().clear();
            player.getInventory().setItem(0, CustomItem.SLENDERMAN_WEAPON.toItemStack());
            player.getInventory().setItem(1, CustomItem.SLENDERMAN_RADAR.toItemStack());
            player.getAttribute(me.dreamdevs.slender.utils.AttributeUtils.getMaxHealth()).setBaseValue(Config.SLENDERMAN_HEALTH.toInt());
            player.setHealth(Config.SLENDERMAN_HEALTH.toInt());
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, Integer.MAX_VALUE));
            if (Config.USE_DISGUISE.toBoolean()) {
                GamePlayer gameSlenderMan = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
                DisguiseManager.disguise(player, gameSlenderMan.getEquippedSkin());
            }
        } else if (role == Role.SURVIVOR || role == Role.DOWNED) {
            // Restore survivor state
            player.teleport(survivorsLocations.get(Util.getRandomNumber(survivorsLocations.size())), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.getInventory().clear();
            ItemStack weapon = CustomItem.SURVIVOR_WEAPON.toItemStack();
            player.getInventory().setItem(0, weapon);
            if (this.flashlightManager != null) {
                this.flashlightManager.giveFlashlight(player, 1);
            }
            me.dreamdevs.slender.compat.VersionCompat.applyDarkness(player, Integer.MAX_VALUE, 0);
        } else if (role == Role.SPECTATOR) {
            // Stay as spectator
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.getInventory().clear();
            
            // Critical cleanup: ensure no survivor effects or downed barriers remain
            if (this.revivalManager != null) {
                this.revivalManager.cleanupDownedState(player);
                this.revivalManager.cancelRevival(player);
            }
            if (this.sanityManager != null) {
                this.sanityManager.removePlayer(player);
            }
            
            if (this.slenderMan != null) {
                player.teleport(this.slenderMan.getLocation());
            }
        }
        
        if (markedCheaters.contains(uuid)) {
            sendMessage("&a" + player.getName() + " has reconnected (as spectator).");
        } else {
            sendMessage("&a" + player.getName() + " has reconnected!");
        }
    }

    public void markCombatHit(UUID uuid) {
        this.lastCombatHit.put(uuid, System.currentTimeMillis());
    }

    public void endGame(Role winner) {
        setArenaState(ArenaState.ENDING);
        setTimer(Config.ENDING_TIMER.toInt());
        this.bossBar.setTitle(Langauge.ARENA_BOSS_BAR_ENDING_TITLE.toString());
        
        String title;
        String subtitle;
        
        if (winner == Role.SURVIVOR) {
            title = Config.WIN_SURVIVORS_TITLE.toString();
            subtitle = Config.WIN_SURVIVORS_SUBTITLE.toString();
            if (Config.WIN_EFFECTS_SOUNDS.toBoolean()) {
                players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f));
            }
            if (Config.WIN_EFFECTS_PARTICLES.toBoolean()) {
                players.keySet().forEach(p -> p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1));
            }
        } else {
            title = Config.WIN_SLENDER_TITLE.toString();
            subtitle = Config.WIN_SLENDER_SUBTITLE.toString();
            if (Config.WIN_EFFECTS_SOUNDS.toBoolean()) {
                players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.5f));
                players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.8f));
            }
            if (Config.WIN_EFFECTS_PARTICLES.toBoolean()) {
                players.keySet().forEach(p -> p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.05));
                players.keySet().forEach(p -> p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.02));
            }
        }

        sendTitleToAllPlayers(title, subtitle, 10, 60, 20);
        
        SlenderGameEndEvent slenderGameEndEvent = new SlenderGameEndEvent(this);
        Bukkit.getPluginManager().callEvent(slenderGameEndEvent);
    }

    public void spawnPage() {
        if (pagesLocations.isEmpty()) return;
        
        removeCurrentPage();
        
        Location loc = pagesLocations.get(Util.getRandomNumber(pagesLocations.size()));
        if (loc == null || loc.getWorld() == null) return;
        
        this.currentPageLocation = loc.clone();
        
        // Use SKELETON_SKULL - white head as requested
        org.bukkit.block.Block block = loc.getBlock();
        block.setType(Material.SKELETON_SKULL, false);
        
        // Spawn professional hologram
        Location holoLoc = block.getLocation().clone().add(0.5, 0.6, 0.5);
        this.pageHologram = loc.getWorld().spawn(holoLoc, ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setCustomNameVisible(true);
            
            String text = "&f&lPAGE &c&l#" + (collectedPages + 1);
            armorStand.customName(ColourUtil.colorizeToComponent(text));
        });

        // Sensory feedback: notify all players that a page has appeared
        players.keySet().forEach(p -> {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.7f, 0.5f);
            p.sendActionBar(Langauge.ARENA_PAGE_APPEARED_ACTIONBAR.toString());
        });

        // Vibrant portal particles around the new page
        loc.getWorld().spawnParticle(Particle.PORTAL, holoLoc, 100, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.ENCHANT, holoLoc, 50, 0.5, 0.5, 0.5, 0.1);
    }
    
    public void removeCurrentPage() {
        if (currentPageLocation != null && currentPageLocation.getWorld() != null) {
            if (escapeLocation != null) {
                escapeLocation.getBlock().setType(Material.AIR);
                escapeLocation.getBlock().getRelative(BlockFace.UP).setType(Material.AIR);
            }
            currentPageLocation.getBlock().setType(Material.AIR);
            currentPageLocation = null;
        }
        if (pageHologram != null) {
            pageHologram.remove();
            pageHologram = null;
        }
    }
    private final List<Entity> escapeRoomEntities = new ArrayList<>();
    private final Map<Location, Material> originalBlocks = new HashMap<>();

    public void spawnEscapeRoomObjectives() {
        // 1. Spawning Generators
        for (Location loc : generatorLocations) {
            originalBlocks.put(loc, loc.getBlock().getType());
            loc.getBlock().setType(Material.IRON_BLOCK);
            
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(0.5, 1, 0.5), EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setMarker(true);
            as.customName(ColourUtil.colorizeToComponent("&e&lGENERATOR"));
            as.setCustomNameVisible(true);
            as.setGravity(false);
            escapeRoomEntities.add(as);
            generatorProgress.put(loc, 0.0);
        }

        // 2. Spawning Keys
        for (Location loc : keyLocations) {
            originalBlocks.put(loc, loc.getBlock().getType());
            loc.getBlock().setType(Material.CHEST);
            
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(0.5, 0.2, 0.5), EntityType.ARMOR_STAND);
            as.customName(ColourUtil.colorizeToComponent("&b&lMASTER KEY"));
            as.setCustomNameVisible(true);
            as.setGravity(false);
            as.setVisible(false);
            as.setMarker(true);
            escapeRoomEntities.add(as);
        }

        // 3. Spawning Escape Point
        if (escapeLocation != null) {
            originalBlocks.put(escapeLocation, escapeLocation.getBlock().getType());
            if (escapeLocation != null) {
                Block bottom = escapeLocation.getBlock();
                Block top = bottom.getRelative(BlockFace.UP);
                
                // Clear blocks first
                bottom.setType(Material.AIR);
                top.setType(Material.AIR);
                
                // Set Bottom
                bottom.setType(Material.IRON_DOOR, false);
                Door doorBottom = (Door) bottom.getBlockData();
                doorBottom.setHalf(Bisected.Half.BOTTOM);
                doorBottom.setFacing(BlockFace.NORTH); // Default toward north
                bottom.setBlockData(doorBottom, true);
                
                // Set Top
                top.setType(Material.IRON_DOOR, false);
                Door doorTop = (Door) top.getBlockData();
                doorTop.setHalf(Bisected.Half.TOP);
                doorTop.setFacing(BlockFace.NORTH); // Must match bottom
                top.setBlockData(doorTop, true);
            }
            ArmorStand as = (ArmorStand) escapeLocation.getWorld().spawnEntity(escapeLocation.clone().add(0.5, 1, 0.5), EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setMarker(true);
            as.customName(ColourUtil.colorizeToComponent("&6&lESCAPE POINT"));
            as.setCustomNameVisible(true);
            as.setGravity(false);
            escapeRoomEntities.add(as);
        }
    }

    public void cleanupEscapeRoomObjectives() {
        for (Entity entity : escapeRoomEntities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        escapeRoomEntities.clear();

        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
        originalBlocks.clear();
    }

    public boolean isReady() {
        if (slenderManSpawnLocation == null) return false;
        if (arenaType == ArenaType.ESCAPE_ROOM) {
            return escapeLocation != null && !generatorLocations.isEmpty() && !keyLocations.isEmpty();
        }
        return !survivorsLocations.isEmpty() && !pagesLocations.isEmpty();
    }

    public void checkSurvivorsRemaining() {
        if (arenaState != ArenaState.RUNNING) return;
        
        long now = System.currentTimeMillis();
        long survivorsLeft = persistentRoles.entrySet().stream()
                .filter(e -> e.getValue() == me.dreamdevs.slender.api.game.Role.SURVIVOR || e.getValue() == me.dreamdevs.slender.api.game.Role.DOWNED)
                .filter(e -> {
                    java.util.UUID id = e.getKey();
                    if (players.keySet().stream().anyMatch(p -> p.getUniqueId().equals(id))) return true;
                    if (disconnectTimes.containsKey(id) && (now - disconnectTimes.get(id) < 30000)) {
                         return !markedCheaters.contains(id);
                    }
                    return false;
                })
                .count();

        if (survivorsLeft == 0) {
            sendMessage("&cAll survivors have been eliminated! Slenderman wins.");
            endGame(me.dreamdevs.slender.api.game.Role.SLENDER);
        }
    }
}

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
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;

@Getter @Setter
public class Arena extends BukkitRunnable implements IArena {

    private final String id;
    private int minPlayers;
    private int maxPlayers;
    private int gameTime;
    private int timer;
    private Location slenderManSpawnLocation;
    private List<Location> survivorsLocations;
    private List<Location> pagesLocations;
    private ArenaState arenaState;
    private Map<Player, Role> players;
    private Player slenderMan;

    private BossBar bossBar;

    private int collectedPages;

    private Scoreboard scoreboard;
    private Objective objective;

    private File file;

    private BukkitTask radiusTask;
    private TerrorRadiusManager terrorRadius;
    private SanityManager sanityManager;
    private StealthManager stealthManager;
    private AmbientSoundManager ambientSoundManager;

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
                if(timer == 0) {
                    endGame(Role.SLENDER);
                    return;
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
                // Only run restart logic once - set WAITING after delay
                break;
        }
    }

    public void start() {
        this.bossBar.setTitle(ColourUtil.colorize(Langauge.ARENA_BOSS_BAR_RUNNING_TITLE.toString().replace("%TIME%", String.valueOf(timer))));
        sendTitleToAllPlayers(Langauge.ARENA_TITLE.toString(), Langauge.ARENA_STARTED_SUBTITLE.toString(), 10, 30, 10);
        sendPlayersToGame();
        setArenaState(ArenaState.RUNNING);
        setTimer(gameTime);
        spawnPage();
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
            // Start Tracking perk for players who have it equipped
            me.dreamdevs.slender.game.perks.Tracking tracking = SlenderMain.getInstance().getPerkManager().getPerksByRole(Role.SURVIVOR)
                    .stream().filter(p -> p instanceof me.dreamdevs.slender.game.perks.Tracking)
                    .map(p -> (me.dreamdevs.slender.game.perks.Tracking) p).findFirst().orElse(null);
            if (tracking != null) {
                players.keySet().stream()
                        .filter(p -> {
                            me.dreamdevs.slender.database.data.GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(p);
                            return gp != null && gp.getPerk(Role.SURVIVOR) instanceof me.dreamdevs.slender.game.perks.Tracking;
                        })
                        .forEach(p -> tracking.startTracking(p, this));
            }
        }, 20L);
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

        final Player finalSlenderMan = this.slenderMan;
        this.scoreboard.getTeam("slenderman").addPlayer(finalSlenderMan);
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
            player.teleport(survivorsLocations.get(Util.getRandomNumber(survivorsLocations.size())), PlayerTeleportEvent.TeleportCause.PLUGIN);
            this.scoreboard.getTeam("survivors").addPlayer(player);
            // Total darkness: darkness effect
            me.dreamdevs.slender.compat.VersionCompat.applyDarkness(player, Integer.MAX_VALUE, 4);
            player.getInventory().clear();
            player.getInventory().setItem(0, CustomItem.SURVIVOR_WEAPON.toItemStack());
            // Lantern with 5 uses
            ItemStack lantern = new ItemStack(Material.LANTERN);
            ItemMeta lMeta = lantern.getItemMeta();
            lMeta.displayName(ColourUtil.colorizeToComponent("&e&lSurvivor Lantern"));
            lMeta.lore(ColourUtil.colouredLoreToComponents(Arrays.asList("&7Right-click to illuminate", "&7Uses: 5/5")));
            lMeta.setUnbreakable(true);
            lantern.setItemMeta(lMeta);
            player.getInventory().setItem(1, lantern);
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
                .filter(entry -> entry.getValue() == Role.SURVIVOR)
                .count();
    }

    public List<Player> getSurvivors() {
        return players.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.SURVIVOR)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void restart() {
        setArenaState(ArenaState.RESTARTING);
        this.bossBar.removeAll();
        // Remove entities/items
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream()
                .filter(entity -> entity instanceof Item)
                .forEach(Entity::remove));
        
        // Reset players
        players.keySet().forEach(player -> {
            SlenderMain.getInstance().getPlayerManager().sendToLobby(player);
        });

        players.clear();
        this.slenderMan = null;
        this.collectedPages = 0;
        setArenaState(ArenaState.WAITING);
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
        Location loc = pagesLocations.get(Util.getRandomNumber(pagesLocations.size()));
        if (loc == null || loc.getWorld() == null) return;
        
        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta meta = pageItem.getItemMeta();
        if (meta != null) {
            meta.displayName(ColourUtil.colorizeToComponent(Langauge.ARENA_PAGE_NUMBER.toString().replace("%NUMBER%", String.valueOf(collectedPages + 1))));
            pageItem.setItemMeta(meta);
        }
        
        Item droppedPage = loc.getWorld().dropItem(loc, pageItem);
        droppedPage.setGlowing(true);
        droppedPage.setUnlimitedLifetime(true);
        
        // In 1.21, ENCHANTMENT_TABLE particle was renamed to ENCHANT
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }
}
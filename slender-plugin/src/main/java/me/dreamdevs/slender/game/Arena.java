package me.dreamdevs.slender.game;

import lombok.Getter;
import lombok.Setter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.*;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.SlenderGameEndEvent;
import me.dreamdevs.slender.api.events.SlenderGameStartEvent;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.IArena;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.compat.VersionCompat;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
        this.scoreboard.registerNewObjective(id, "dummy", id);

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
                    endGame();
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

        this.scoreboard.getTeam("slenderman").addPlayer(slenderMan);
        slenderMan.getInventory().clear();
        slenderMan.getInventory().setItem(0, CustomItem.SLENDERMAN_WEAPON.toItemStack());
        slenderMan.getInventory().setItem(1, CustomItem.SLENDERMAN_RADAR.toItemStack());
        GamePlayer gameSlenderMan = SlenderMain.getInstance().getPlayerManager().getPlayer(slenderMan);
        // Perk ability item
        Perk slenderPerk = gameSlenderMan.getPerk(Role.SLENDER);
        if (slenderPerk != null) {
            PerkInfo sInfo = slenderPerk.getClass().getAnnotation(PerkInfo.class);
            ItemStack perkItem = new ItemStack(sInfo.icon());
            ItemMeta sMeta = perkItem.getItemMeta();
            sMeta.setDisplayName(ColourUtil.colorize("&4&l" + sInfo.name()));
            sMeta.setLore(ColourUtil.colouredLore(Arrays.asList("&7Right-click to activate", "&7Your perk ability")));
            sMeta.setUnbreakable(true);
            perkItem.setItemMeta(sMeta);
            slenderMan.getInventory().setItem(2, perkItem);
        }

        // Darkness effect for all survivors - total blindness
        tempList.forEach(player -> {
            player.teleport(survivorsLocations.get(Util.getRandomNumber(survivorsLocations.size())));
            this.scoreboard.getTeam("survivors").addPlayer(player);
            // Total darkness: high level blindness, no night vision
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 4));
            player.getInventory().clear();
            player.getInventory().setItem(0, CustomItem.SURVIVOR_WEAPON.toItemStack());
            // Lantern with 5 uses
            ItemStack lantern = new ItemStack(Material.LANTERN);
            ItemMeta lMeta = lantern.getItemMeta();
            lMeta.setDisplayName(ColourUtil.colorize("&e&lSurvivor Lantern"));
            lMeta.setLore(ColourUtil.colouredLore(Arrays.asList("&7Right-click to illuminate", "&7Uses: 5/5")));
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
                pMeta.setDisplayName(ColourUtil.colorize("&6&l" + pInfo.name()));
                pMeta.setLore(ColourUtil.colouredLore(Arrays.asList("&7Right-click to activate", "&7Your perk ability")));
                pMeta.setUnbreakable(true);
                perkItem.setItemMeta(pMeta);
                player.getInventory().setItem(2, perkItem);
            }
        });

        slenderMan.teleport(slenderManSpawnLocation);
        slenderMan.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Config.SLENDERMAN_HEALTH.toInt());
        slenderMan.setHealth(Config.SLENDERMAN_HEALTH.toInt());
        slenderMan.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, Integer.MAX_VALUE));
        if (Config.USE_DISGUISE.toBoolean()) {
            SlenderDisguise skin = gameSlenderMan.getEquippedSkin();
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () ->
                    DisguiseManager.disguise(slenderMan, skin), 10L);
        }

    }

    private void sendTitleToAllPlayers(String title, String subTitle, int fadeIn, int stayIn, int fadeOut) {
        if (players.isEmpty())
            return;
        players.keySet().forEach(player -> player.sendTitle(ColourUtil.colorize(title), ColourUtil.colorize(subTitle), fadeIn, stayIn, fadeOut));
    }

    private void sendActionBar(String message) {
        players.keySet().forEach(player -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColourUtil.colorize(message))));
    }

    public void restart() {
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream().filter(Item.class::isInstance).forEach(Entity::remove));

        SlenderMain.getInstance().getPlayerManager().getPlayers().stream().filter(gamePlayer -> gamePlayer.isInArena() && gamePlayer.getArena().equals(this) && (Boolean) gamePlayer.getSetting(Setting.AUTO_JOIN_MODE)).forEach(gamePlayer -> {
            SlenderMain.getInstance().getGameManager().leaveGame(gamePlayer.getPlayer(), this);
            Arena arena = SlenderMain.getInstance().getGameManager().getAvailableArena();
            if(arena == null) {
                gamePlayer.getPlayer().sendMessage(Langauge.ARENA_NO_AVAILABLE_ARENAS.toString());
                return;
            }
            SlenderMain.getInstance().getGameManager().joinGame(gamePlayer.getPlayer(), arena);
        });

        new ArrayList<>(players.keySet()).forEach(player -> SlenderMain.getInstance().getGameManager().leaveGame(player, this));

        setArenaState(ArenaState.RESTARTING);
        players.clear();
        setCollectedPages(0);

        Bukkit.getWorlds().forEach(world -> world.getEntities().stream().filter(Item.class::isInstance).forEach(Entity::remove));
        this.bossBar.setTitle(Langauge.ARENA_BOSS_BAR_WAITING_TITLE.toString());
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.scoreboard.registerNewObjective(id, "dummy", id);
        this.scoreboard.registerNewTeam("survivors");
        this.scoreboard.registerNewTeam("slenderman");
        this.slenderMan = null;
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> setArenaState(ArenaState.WAITING), 100L);
    }

    public void endGame() {
        this.bossBar.setTitle(Langauge.ARENA_BOSS_BAR_RUNNING_TITLE.toString().replace("%TIME%", String.valueOf(timer)));
        if (radiusTask != null) {
            this.radiusTask.cancel();
            this.radiusTask = null;
        }
        if (this.terrorRadius != null) {
            this.terrorRadius.stop();
            this.terrorRadius = null;
        }
        if (this.ambientSoundManager != null) {
            this.ambientSoundManager.stop();
            this.ambientSoundManager = null;
        }
        int pagesToWin = Config.PAGES_TO_WIN.toInt();
        if(getCollectedPages() < pagesToWin) {
            sendTitleToAllPlayers(Langauge.ARENA_TITLE.toString(), Langauge.ARENA_WIN_SLENDERMAN_SUBTITLE.toString(), 10, 50, 10);

            GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(slenderMan);
            gamePlayer.setStatistic(Statistic.WINS, gamePlayer.getStatistic(Statistic.WINS)+1);

        } else {
            sendTitleToAllPlayers(Langauge.ARENA_TITLE.toString(), Langauge.ARENA_WIN_SURVIVORS_SUBTITLE.toString(),10, 50, 10);
            getPlayers().entrySet().stream().filter(playerRoleEntry -> playerRoleEntry.getValue() == Role.SURVIVOR).forEach(playerRoleEntry -> {
                GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(playerRoleEntry.getKey());
                gamePlayer.setStatistic(Statistic.WINS, gamePlayer.getStatistic(Statistic.WINS)+1);
            });
        }

        getPlayers().keySet().forEach(player -> {
            getPlayers().put(player, Role.NONE);
            player.getInventory().clear();
            player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
            player.getInventory().setItem(7, CustomItem.PLAY_AGAIN.toItemStack());
            player.getInventory().setItem(8, CustomItem.LEAVE.toItemStack());
        });

        setArenaState(ArenaState.ENDING);
        setTimer(15);
        SlenderGameEndEvent slenderGameEndEvent = new SlenderGameEndEvent(this);
        Bukkit.getPluginManager().callEvent(slenderGameEndEvent);
    }

    public void spawnPage() {
        ItemStack itemStack = new ItemStack(Material.PAPER);

        Item item = slenderManSpawnLocation.getWorld().dropItem(getPagesLocations().get(Util.getRandomNumber(getPagesLocations().size())), itemStack);
        item.setCustomName(Langauge.ARENA_COLLECTED_PAGES.toString().replace("%NUMBER%", String.valueOf((collectedPages+1))));
        item.setCustomNameVisible(true);

        sendMessage(Langauge.ARENA_PAGE_SPAWNED_INFO.toString());
    }

    public int getSurvivorsAmount() {
        return (int) players.entrySet().stream().filter(playerRoleEntry -> playerRoleEntry.getValue() == Role.SURVIVOR).count();
    }

    public void sendMessage(String message) {
        if (players.isEmpty()) {
            return;
        }
        players.keySet().forEach(player -> player.sendMessage(ColourUtil.colorize(message)));
    }

    public boolean isRunning() {
        return getArenaState() == ArenaState.RUNNING || getArenaState() == ArenaState.ENDING;
    }

    public List<Player> getSurvivors() {
        return players.entrySet().stream()
                .filter(playerRoleEntry -> playerRoleEntry.getValue().equals(Role.SURVIVOR))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void terrorRadius() {
        if(getArenaState() == ArenaState.RUNNING) {
            World world = slenderMan.getWorld();
            world.getNearbyEntities(slenderMan.getLocation(), Config.TERROR_RADIUS.toInt(), Config.TERROR_RADIUS.toInt(), Config.TERROR_RADIUS.toInt())
                    .stream().filter(Player.class::isInstance)
                    .map(Player.class::cast)
                    .filter(player -> getPlayers().containsKey(player) && getPlayers().get(player) != Role.SLENDER)
                    .forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 2f, 2f));
        } else {
            this.radiusTask.cancel();
        }
    }

    @Override
    public Location getSlenderManSpawnLocation() {
        return slenderManSpawnLocation;
    }
}
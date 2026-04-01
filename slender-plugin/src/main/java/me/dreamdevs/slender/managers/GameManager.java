package me.dreamdevs.slender.managers;

import lombok.Getter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.events.SlenderJoinArenaEvent;
import me.dreamdevs.slender.api.events.SlenderQuitArenaEvent;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.CustomItem;
import me.dreamdevs.slender.game.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class GameManager {

    private final List<Arena> arenas;

    public GameManager() {
        this.arenas = new ArrayList<>();
    }

    public void loadGames() {
        File file = new File(SlenderMain.getInstance().getDataFolder(), "arenas");
        if(!file.exists() || !file.isDirectory())
            file.mkdirs();

        Optional.ofNullable(file.listFiles(((dir, name) -> name.endsWith(".yml")))).ifPresent(files -> Arrays.asList(files).forEach(this::loadGame));
    }

    public void joinGame(Player player, Arena arena) {
        if(arena.getArenaState() == ArenaState.WAITING || arena.getArenaState() == ArenaState.STARTING) {
            if(arena.getPlayers().size() >= arena.getMaxPlayers()) {
                player.sendMessage(Langauge.ARENA_NO_SLOTS.toString());
                return;
            }
            GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
            if(gamePlayer.isInArena()) {
                player.sendMessage(Langauge.ARENA_PLAYER_IN_GAME.toString());
                return;
            }

            if(SlenderMain.getInstance().getPartyManager() != null && SlenderMain.getInstance().getPartyManager().isInParty(gamePlayer)) {
                if(!SlenderMain.getInstance().getPartyManager().getParty(gamePlayer).getPartyLeader().equals(gamePlayer.getPlayer())) {
                    player.sendMessage(Langauge.PARTY_PLAYER_NOT_LEADER.toString());
                    return;
                }

                Party party = SlenderMain.getInstance().getPartyManager().getParty(gamePlayer);
                if(arena.getMaxPlayers()-arena.getPlayers().size() < party.getMembersMap().size()) {
                    player.sendMessage(Langauge.PARTY_TOO_MANY_PLAYERS.toString());
                    return;
                }

                party.getMembersMap().keySet().forEach(gameMember -> {
                    gameMember.getPlayer().teleport(arena.getSlenderManSpawnLocation());
                    gameMember.clearInventory();
                    gameMember.getPlayer().setScoreboard(arena.getScoreboard());
                    gameMember.getPlayer().getInventory().setItem(8, CustomItem.LEAVE.toItemStack());
                    gameMember.getPlayer().getInventory().setItem(7, CustomItem.FORCED_START.toItemStack());
                    arena.getPlayers().put(gameMember.getPlayer(), Role.NONE);
                    arena.getBossBar().addPlayer(gameMember.getPlayer());

                    Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                        gameMember.getPlayer().hidePlayer(SlenderMain.getInstance(), onlinePlayer);
                        onlinePlayer.hidePlayer(SlenderMain.getInstance(), gameMember.getPlayer());
                    });

                    arena.getPlayers().keySet().forEach(playerGame -> {
                        gameMember.getPlayer().showPlayer(SlenderMain.getInstance(), playerGame);
                        playerGame.showPlayer(SlenderMain.getInstance(), gameMember.getPlayer());
                    });

                    if ((boolean) gameMember.getSetting(Setting.SHOW_ARENA_JOIN_MESSAGE)) {
                        gameMember.getPlayer().sendMessage(Langauge.ARENA_JOIN_GAME_INFO.toString());
                    }

                    SlenderJoinArenaEvent slenderJoinArenaEvent = new SlenderJoinArenaEvent(gameMember, arena);
                    Bukkit.getPluginManager().callEvent(slenderJoinArenaEvent);

                    if(arena.getPlayers().size() >= arena.getMinPlayers()) {
                        arena.setArenaState(ArenaState.STARTING);
                        arena.sendMessage(Langauge.ARENA_STARTING_INFO.toString());
                        arena.setTimer(30);
                    }
                });
                return;
            }

            player.teleport(arena.getSlenderManSpawnLocation());
            gamePlayer.clearInventory();
            player.setScoreboard(arena.getScoreboard());
            player.getInventory().setItem(8, CustomItem.LEAVE.toItemStack());
            player.getInventory().setItem(7, CustomItem.FORCED_START.toItemStack());
            arena.getPlayers().put(player, Role.NONE);
            arena.getBossBar().addPlayer(player);

            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                player.hidePlayer(SlenderMain.getInstance(), onlinePlayer);
                onlinePlayer.hidePlayer(SlenderMain.getInstance(), player);
            });

            arena.getPlayers().keySet().forEach(playerGame -> {
                player.showPlayer(SlenderMain.getInstance(), playerGame);
                playerGame.showPlayer(SlenderMain.getInstance(), player);
            });

            if ((boolean) gamePlayer.getSetting(Setting.SHOW_ARENA_JOIN_MESSAGE))
                player.sendMessage(Langauge.ARENA_JOIN_GAME_INFO.toString());

            SlenderJoinArenaEvent slenderJoinArenaEvent = new SlenderJoinArenaEvent(gamePlayer, arena);
            Bukkit.getPluginManager().callEvent(slenderJoinArenaEvent);

            if (arena.getPlayers().size() >= arena.getMinPlayers()) {
                arena.setArenaState(ArenaState.STARTING);
                arena.sendMessage(Langauge.ARENA_STARTING_INFO.toString());
                arena.setTimer(30);
                List<Player> lobbyPlayers = SlenderMain.getInstance().getPlayerManager().getPlayers().stream().filter(lobbyGamePlayer -> !lobbyGamePlayer.isInArena()).map(GamePlayer::getPlayer).collect(Collectors.toList());
                sendArenaAnnouncement(arena, Langauge.LOBBY_ARENA_STARTING_ANNOUNCEMENT.toString()
                        .replace("%ARENA%", arena.getId()), Langauge.LOBBY_CLICK_HERE.toString(), lobbyPlayers);
            }
        } else {
            player.sendMessage(Langauge.ARENA_STILL_RUNNING.toString());
        }
    }

    public void leaveGame(Player player, Arena arena) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

        if (!forceRemovePlayerFromGame(gamePlayer)) {
            gamePlayer.getPlayer().sendMessage(Langauge.ARENA_PLAYER_IN_GAME.toString());
            return;
        }

        if (SlenderMain.getInstance().getPartyManager() != null && SlenderMain.getInstance().getPartyManager().isInParty(gamePlayer) && SlenderMain.getInstance().getPartyManager().getParty(gamePlayer).getPartyLeader().equals(gamePlayer.getPlayer())) {
            Party party = SlenderMain.getInstance().getPartyManager().getParty(gamePlayer);
            party.getMembers().stream().map(GamePlayer.class::cast).forEach(this::forceRemovePlayerFromGame);
        }

        SlenderQuitArenaEvent slenderQuitArenaEvent = new SlenderQuitArenaEvent(gamePlayer, arena);
        Bukkit.getPluginManager().callEvent(slenderQuitArenaEvent);

        if (arena.getPlayers().size() < arena.getMinPlayers()) {
            if (arena.getArenaState() == ArenaState.STARTING) {
                arena.setArenaState(ArenaState.WAITING);
                arena.sendMessage(Langauge.ARENA_STOPPED_STARTING.toString());
                arena.setTimer(0);
            }
        }
    }

    public void loadGame(File file) {
        Util.createFile(file);
        String fileName = file.getName();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        fileName = fileName.substring(0, fileName.length()-4);
        Arena arena = new Arena(fileName);
        arena.setFile(file);
        arena.setMinPlayers(configuration.getInt("GameSettings.MinPlayers"));
        arena.setMaxPlayers(configuration.getInt("GameSettings.MaxPlayers"));
        arena.setGameTime(configuration.getInt("GameSettings.Time"));
        arena.setSlenderManSpawnLocation(Util.getStringLocation(configuration.getString("GameSettings.SlenderStartLocation"), true));
        List<Location> locations = new ArrayList<>();
        for(String s : configuration.getStringList("GameSettings.SurvivorsLocations"))
            locations.add(Util.getStringLocation(s, true));
        arena.setSurvivorsLocations(locations);
        List<Location> locations1 = new ArrayList<>();
        for(String s : configuration.getStringList("GameSettings.PagesLocations"))
            locations1.add(Util.getStringLocation(s, true));
        arena.setPagesLocations(locations1);
        arena.startGame();
        arenas.add(arena);
    }

    public void saveGame(Arena arena) {
        String fileName = arena.getId()+".yml";
        File f = new File(SlenderMain.getInstance().getDataFolder(), "arenas/"+fileName);
        Util.createFile(f);

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(f);
        configuration.set("GameSettings.Time", arena.getGameTime());
        configuration.set("GameSettings.MinPlayers", arena.getMinPlayers());
        configuration.set("GameSettings.MaxPlayers", arena.getMaxPlayers());
        configuration.set("GameSettings.SlenderStartLocation", Util.getLocationString(arena.getSlenderManSpawnLocation(), true));
        List<String> locations = new ArrayList<>();
        for (Location location : arena.getSurvivorsLocations()) {
            String line = Util.getLocationString(location, true);
            locations.add(line);
        }
        configuration.set("GameSettings.SurvivorsLocations", locations);
        List<String> locations1 = new ArrayList<>();
        for (Location location : arena.getPagesLocations()) {
            String line = Util.getLocationString(location, true);
            locations1.add(line);
        }
        configuration.set("GameSettings.PagesLocations", locations1);
        try {
            configuration.save(f);
        } catch (IOException ignored) { }
    }

    public Arena getAvailableArena() {
        return arenas.stream()
                .filter(arena -> !arena.isRunning() && arena.getSurvivorsAmount() != arena.getMaxPlayers())
                .findAny()
                .orElse(null);
    }

    public boolean isInArena(Player player) {
        return arenas.stream()
                .filter(arena -> arena.getPlayers().containsKey(player))
                .findFirst()
                .orElse(null) != null;
    }

    public Arena getArena(String id) {
        return arenas.stream()
                .filter(arena -> arena.getId().equalsIgnoreCase(id))
                .findAny()
                .orElse(null);
    }

    public void saveGames() {
        Optional.ofNullable(arenas).ifPresent(games1 -> games1.forEach(this::saveGame));
    }

    public boolean forceRemovePlayerFromGame(GamePlayer gamePlayer) {
        if(!gamePlayer.isInArena()) {
            Util.sendPluginMessage("&cCould not remove player from the game!");
            return false;
        }
        Arena arena = (Arena) gamePlayer.getArena();
        SlenderMain.getInstance().getPlayerManager().sendToLobby(gamePlayer.getPlayer());
        SlenderMain.getInstance().getPlayerManager().loadLobby(gamePlayer.getPlayer());
        arena.getPlayers().remove(gamePlayer.getPlayer());
        arena.getBossBar().removePlayer(gamePlayer.getPlayer());
        if(arena.getPlayers().size() == 0 && arena.getArenaState() == ArenaState.RUNNING) {
            arena.endGame();
        }
        return true;
    }

    private void sendArenaAnnouncement(Arena arena, String message, String hoverMessage, List<Player> players) {
        Component component = ColourUtil.colorizeToComponent(message)
                .hoverEvent(HoverEvent.showText(ColourUtil.colorizeToComponent(hoverMessage)))
                .clickEvent(ClickEvent.runCommand("/slender join " + arena.getId()));

        players.forEach(player -> player.sendMessage(component));
    }

}
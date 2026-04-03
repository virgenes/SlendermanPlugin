package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

public class PlayerListeners implements Listener {

    @EventHandler
    public void joinPlayer(PlayerJoinEvent event) {
        event.joinMessage(null);

        if (SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer()) == null) {
            GamePlayer gamePlayer = new GamePlayer(event.getPlayer());
            gamePlayer.setSetting(Setting.AUTO_JOIN_MODE, false);
            gamePlayer.setSetting(Setting.MESSAGE_TYPE, "all");
            gamePlayer.setSetting(Setting.SHOW_ARENA_JOIN_MESSAGE, true);
            gamePlayer.setSetting(Setting.MUSIC_ENABLED, true);
            SlenderMain.getInstance().getPlayerManager().getPlayers().add(gamePlayer);
        }

        SlenderMain.getInstance().getPlayerManager().sendToLobby(event.getPlayer());
        SlenderMain.getInstance().getPlayerManager().loadLobby(event.getPlayer());
    }

    @EventHandler
    public void quitPlayer(PlayerQuitEvent event) {
        event.quitMessage(null);

        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
        if(gamePlayer.isInArena()) {
            Arena arena = (Arena) gamePlayer.getArena();

            if(arena.getPlayers().get(gamePlayer.getPlayer()) == Role.SLENDER) {
                arena.sendMessage(ColourUtil.colorizeToComponent(Langauge.ARENA_SLENDER_MAN_LEFT.toString()));
                arena.getPlayers().remove(gamePlayer.getPlayer());
                if(arena.getArenaState() == ArenaState.RUNNING || arena.getArenaState() == ArenaState.STARTING) {
                    arena.restart();
                }
            } else {
                arena.getPlayers().remove(gamePlayer.getPlayer());
            }
        }

        SlenderMain.getInstance().getPlayerManager().getPlayers().remove(gamePlayer);
    }

    @EventHandler
    public void chatEvent(io.papermc.paper.event.player.AsyncChatEvent event) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
        event.viewers().clear();
        if(gamePlayer.isInArena()) {
            Arena arena = (Arena) gamePlayer.getArena();
            arena.getPlayers().keySet().stream().map(player -> SlenderMain.getInstance().getPlayerManager().getPlayer(player))
                    .filter(arenaPlayer -> arenaPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("all")
                            || arenaPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("arena"))
                    .forEach(player -> event.viewers().add(player.getPlayer()));
        } else {
            Bukkit.getOnlinePlayers().stream().map(player -> SlenderMain.getInstance().getPlayerManager().getPlayer(player))
                    .filter(lobbyPlayer -> !lobbyPlayer.isInArena()
                            && (lobbyPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("all") ||
                            lobbyPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("lobby")))
                    .forEach(player -> event.viewers().add(player.getPlayer()));
        }
    }

    @EventHandler
    public void changeFoodEvent(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void dropEvent(PlayerDropItemEvent event) {
        if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }
}
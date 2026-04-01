package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PlayerListeners implements Listener {

    @EventHandler
    public void joinPlayer(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        if (SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer()) == null) {
            GamePlayer gamePlayer = new GamePlayer(event.getPlayer());
            gamePlayer.setSetting(Setting.AUTO_JOIN_MODE, false);
            gamePlayer.setSetting(Setting.MESSAGE_TYPE, "all");
            gamePlayer.setSetting(Setting.SHOW_ARENA_JOIN_MESSAGE, true);
            SlenderMain.getInstance().getPlayerManager().getPlayers().add(gamePlayer);
        }

        SlenderMain.getInstance().getPlayerManager().sendToLobby(event.getPlayer());
        SlenderMain.getInstance().getPlayerManager().loadLobby(event.getPlayer());
    }

    private static class TestItem extends MenuItem {

        public TestItem(String displayName, ItemStack icon, String... lore) {
            super(displayName, icon, lore);
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.getPlayer().sendMessage("TEST!");
        }
    }

    @EventHandler
    public void quitPlayer(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
        if(gamePlayer.isInArena()) {
            Arena arena = (Arena) gamePlayer.getArena();

            if(arena.getPlayers().get(gamePlayer.getPlayer()) == Role.SLENDER) {
                arena.sendMessage(Langauge.ARENA_SLENDER_MAN_LEFT.toString());
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
    public void chatEvent(AsyncPlayerChatEvent event) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
        event.getRecipients().clear();
        if(gamePlayer.isInArena()) {
            Arena arena = (Arena) gamePlayer.getArena();
            arena.getPlayers().keySet().stream().map(player -> SlenderMain.getInstance().getPlayerManager().getPlayer(player))
                    .filter(arenaPlayer -> arenaPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("all")
                            || arenaPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("arena"))
                    .forEach(player -> event.getRecipients().add(player.getPlayer()));
        } else {
            Bukkit.getOnlinePlayers().stream().map(player -> SlenderMain.getInstance().getPlayerManager().getPlayer(player))
                    .filter(lobbyPlayer -> !lobbyPlayer.isInArena()
                            && (lobbyPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("all") ||
                            lobbyPlayer.getSetting(Setting.MESSAGE_TYPE).toString().equalsIgnoreCase("lobby")))
                    .forEach(player -> event.getRecipients().add(player.getPlayer()));
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
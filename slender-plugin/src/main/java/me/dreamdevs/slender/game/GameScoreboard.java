package me.dreamdevs.slender.game;

import org.bukkit.entity.Player;

/**
 * Manages the in-game scoreboard display for all players in an arena.
 */
public class GameScoreboard {

    private final Arena arena;

    public GameScoreboard(Arena arena) {
        this.arena = arena;
    }

    public void start() {
        update();
    }

    public void stop() {
        for (Player player : arena.getPlayers().keySet()) {
            if (player.isOnline()) {
                player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    public void update() {
        for (Player player : arena.getPlayers().keySet()) {
            if (player.isOnline()) {
                player.setScoreboard(arena.getScoreboard());
            }
        }
    }
}

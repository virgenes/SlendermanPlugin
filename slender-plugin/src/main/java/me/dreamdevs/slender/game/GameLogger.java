package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs game events to the server console and optionally to a log file.
 */
public class GameLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logGameStart(Arena arena) {
        String time = LocalDateTime.now().format(FORMATTER);
        SlenderMain.getInstance().getLogger().info(
                "[" + time + "] Game started in arena '" + arena.getId() + "' with "
                        + arena.getPlayers().size() + " players.");
    }

    public static void logGameEnd(Arena arena, boolean survivorsWin) {
        String time = LocalDateTime.now().format(FORMATTER);
        String result = survivorsWin ? "SURVIVORS WIN" : "SLENDERMAN WINS";
        SlenderMain.getInstance().getLogger().info(
                "[" + time + "] Game ended in arena '" + arena.getId() + "' — " + result + ".");
    }
}

package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.music.NbsParser;
import me.dreamdevs.slender.music.NbsPlayer;
import me.dreamdevs.slender.music.NbsSong;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

/**
 * High-level manager built on top of our custom NBS engine.
 *
 * Track logic:
 * - {@code initial.nbs} plays on loop from game start.
 * - When a survivor hits Slenderman, {@code combat.nbs} kicks in.
 * - When {@code combat.nbs} ends, reverts automatically to {@code initial.nbs}.
 */
public class MusicManager {

    private static final String MUSIC_DIR = "music";

    private final Arena arena;
    private final NbsSong initialSong;
    private final NbsSong combatSong;

    private NbsPlayer currentPlayer;
    private boolean inCombat = false;
    private boolean stopped  = false;

    public MusicManager(Arena arena) {
        this.arena = arena;
        this.initialSong = load("initial.nbs");
        this.combatSong  = load("combat.nbs");
        SlenderMain.getInstance().getLogger().info("[Slender] Professional Music Engine Initialized (Independent).");
    }

    // ------------------------------------------------------------------

    /** Starts the initial track for all eligible arena players. */
    public void start() {
        stopped  = false;
        inCombat = false;
        playInitial();
    }

    /**
     * Briefly switches to the combat track. Automatically reverts to initial
     * when the combat track finishes. Safe to call even if already in combat.
     */
    public void triggerCombat() {
        if (inCombat || stopped) return;
        if (combatSong == null) return;
        inCombat = true;
        stopCurrent();

        NbsPlayer sp = new NbsPlayer(combatSong);
        addEligiblePlayers(sp);
        sp.start(SlenderMain.getInstance(), () -> {
            if (!stopped) {
                inCombat = false;
                playInitial();
            }
        });
        currentPlayer = sp;
    }

    /** Stops all music and releases resources. */
    public void stop() {
        stopped = true;
        stopCurrent();
    }

    /** Adds a player mid-game (e.g. after respawn). */
    public void addPlayer(Player player) {
        if (stopped || currentPlayer == null || !currentPlayer.isPlaying()) return;
        if (musicEnabled(player)) currentPlayer.addPlayer(player);
    }

    /** Removes a player that went spectator or left. */
    public void removePlayer(Player player) {
        if (currentPlayer != null) currentPlayer.removePlayer(player);
    }

    // ------------------------------------------------------------------

    private void playInitial() {
        if (stopped || initialSong == null) return;
        stopCurrent();

        NbsPlayer sp = new NbsPlayer(initialSong);
        addEligiblePlayers(sp);
        sp.start(SlenderMain.getInstance(), () -> {
            // Loop: when the track ends, start it again
            if (!stopped && !inCombat && arena.isRunning()) {
                playInitial();
            }
        });
        currentPlayer = sp;
    }

    private void stopCurrent() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer = null;
        }
    }

    private void addEligiblePlayers(NbsPlayer player) {
        for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
            Player p = entry.getKey();
            if (p.isOnline() && musicEnabled(p)) {
                player.addPlayer(p);
            }
        }
    }

    public boolean musicEnabled(Player player) {
        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gp == null) return true;
        Object val = gp.getSetting(Setting.MUSIC_ENABLED);
        return val == null || (boolean) val;
    }

    // ------------------------------------------------------------------

    /** Extracts the NBS resource from the JAR to the data folder, then parses it. */
    private NbsSong load(String name) {
        File musicDir = new File(SlenderMain.getInstance().getDataFolder(), MUSIC_DIR);
        if (!musicDir.exists()) musicDir.mkdirs();
        File dest = new File(musicDir, name);

        if (!dest.exists()) {
            String resource = MUSIC_DIR + "/" + name;
            try (InputStream in = SlenderMain.getInstance().getResource(resource)) {
                if (in != null) {
                    Files.copy(in, dest.toPath());
                } else {
                    SlenderMain.getInstance().getLogger().warning("[MusicManager] Missing resource: " + resource);
                    return null;
                }
            } catch (Exception e) {
                SlenderMain.getInstance().getLogger().warning("[MusicManager] Extract failed for " + name + ": " + e.getMessage());
                return null;
            }
        }

        NbsSong song = NbsParser.parse(dest, SlenderMain.getInstance().getLogger());
        if (song == null || !song.hasNotes()) {
            SlenderMain.getInstance().getLogger().warning("[MusicManager] Parsed empty or invalid song: " + name);
            return null;
        }
        return song;
    }
}

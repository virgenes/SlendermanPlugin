package me.dreamdevs.slender.music;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plays an {@link NbsSong} to a set of Bukkit players using a per-game-tick
 * scheduler. All sounds are played via {@link Player#playSound}, so no external
 * dependencies are required.
 *
 * Pitch formula: 2^((key - 45) / 12)
 *   key 33 (F#3) → pitch 0.5
 *   key 45 (F#4) → pitch 1.0
 *   key 57 (F#5) → pitch 2.0
 */
public class NbsPlayer {

    // Maps NBS instrument indices 0-15 to Bukkit Sound enum values.
    // Order matches the vanilla NBS spec.
    private static final Sound[] INSTRUMENTS = {
            sound("block.note_block.harp"),        // 0  Piano / Harp
            sound("block.note_block.bass"),         // 1  Double Bass
            sound("block.note_block.basedrum"),     // 2  Bass Drum
            sound("block.note_block.snare"),        // 3  Snare Drum
            sound("block.note_block.hat"),          // 4  Click / Hat
            sound("block.note_block.guitar"),       // 5  Guitar
            sound("block.note_block.flute"),        // 6  Flute
            sound("block.note_block.bell"),         // 7  Bell
            sound("block.note_block.chime"),        // 8  Chime
            sound("block.note_block.xylophone"),    // 9  Xylophone
            sound("block.note_block.iron_xylophone"),// 10 Iron Xylophone
            sound("block.note_block.cow_bell"),     // 11 Cow Bell
            sound("block.note_block.didgeridoo"),   // 12 Didgeridoo
            sound("block.note_block.bit"),          // 13 Bit
            sound("block.note_block.banjo"),        // 14 Banjo
            sound("block.note_block.pling"),        // 15 Pling
    };

    /** Callback fired when the song finishes naturally. */
    public interface SongEndCallback {
        void onEnd();
    }

    private final NbsSong song;
    private final List<Player> players = new CopyOnWriteArrayList<>();

    private BukkitTask task;
    private long startTimeMs;
    private int lastPlayedTick;
    private SongEndCallback endCallback;

    public NbsPlayer(NbsSong song) {
        this.song = song;
    }

    /**
     * Begin playback. Fires every game tick (50 ms) and advances through the
     * song using real wall-clock time so timing stays accurate regardless of
     * server TPS fluctuations.
     */
    public void start(Plugin plugin, SongEndCallback callback) {
        stop();
        this.endCallback  = callback;
        this.startTimeMs  = System.currentTimeMillis();
        this.lastPlayedTick = -1;

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isPlaying() {
        return task != null;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void clearPlayers() {
        players.clear();
    }

    // ------------------------------------------------------------------ //

    private void tick() {
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        // song-tick = elapsed seconds * song tempo (ticks/sec)
        int currentTick = (int) (elapsedMs / 1000.0 * song.getTempo());

        // Song finished
        if (currentTick > song.getLength()) {
            stop();
            if (endCallback != null) endCallback.onEnd();
            return;
        }

        // Play every song-tick we haven't played yet, in order
        for (int t = lastPlayedTick + 1; t <= currentTick; t++) {
            List<NbsNote> notes = song.getNotesAt(t);
            for (NbsNote note : notes) {
                playNote(note);
            }
        }
        lastPlayedTick = currentTick;
    }

    private void playNote(NbsNote note) {
        Sound sound = resolveInstrument(note.instrument);
        // Clamped to Minecraft's 0.5–2.0 pitch range
        float pitch = (float) Math.pow(2.0, (note.key - 45) / 12.0);
        pitch = Math.max(0.5f, Math.min(2.0f, pitch));

        for (Player p : players) {
            if (p.isOnline()) {
                p.playSound(p.getLocation(), sound, note.volume, pitch);
            }
        }
    }

    private static Sound resolveInstrument(int index) {
        if (index >= 0 && index < INSTRUMENTS.length && INSTRUMENTS[index] != null) {
            return INSTRUMENTS[index];
        }
        return Sound.BLOCK_NOTE_BLOCK_HARP; // safe fallback
    }

    /**
     * Looks up a Sound by its Bukkit namespaced key string, with a graceful
     * fallback so the plugin never crashes on older server versions missing a
     * specific sound.
     */
    private static Sound sound(String key) {
        String enumName = key.replace('.', '_').toUpperCase();

        try {
            // Use reflection to get the value. This avoids IncompatibleClassChangeError
            // which happens when the compiler and runtime disagree on whether Sound 
            // is a class or an interface (common in Paper 1.21+ remaps).
            return (Sound) Sound.class.getMethod("valueOf", String.class).invoke(null, enumName);
        } catch (Exception e) {
            // If valueOf fails, try searching the fields (fallback for some edge cases)
            try {
                return (Sound) Sound.class.getField(enumName).get(null);
            } catch (Exception e2) {
                return Sound.BLOCK_NOTE_BLOCK_HARP;
            }
        }
    }
}

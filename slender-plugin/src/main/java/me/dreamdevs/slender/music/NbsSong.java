package me.dreamdevs.slender.music;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a fully parsed NBS (Note Block Studio) song.
 * Stores notes grouped by song-tick for efficient playback.
 */
public class NbsSong {

    private final float tempo;    // notes per second (e.g. 10.0 = 10 ticks/sec)
    private final int length;     // total length in song-ticks
    private final Map<Integer, List<NbsNote>> notesByTick; // tick -> list of notes

    public NbsSong(float tempo, int length, Map<Integer, List<NbsNote>> notesByTick) {
        this.tempo = tempo;
        this.length = length;
        this.notesByTick = notesByTick;
    }

    public float getTempo() { return tempo; }
    public int getLength() { return length; }

    public List<NbsNote> getNotesAt(int tick) {
        return notesByTick.getOrDefault(tick, Collections.emptyList());
    }

    public boolean hasNotes() {
        return !notesByTick.isEmpty();
    }
}

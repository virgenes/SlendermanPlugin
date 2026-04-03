package me.dreamdevs.slender.music;

/**
 * A single note from an NBS file.
 */
public class NbsNote {

    public final int instrument; // 0-15 vanilla instruments
    public final int key;        // 0-87 (MIDI key, 33=F#3, 57=F#5)
    public final float volume;   // 0.0 - 1.0

    public NbsNote(int instrument, int key, float volume) {
        this.instrument = instrument;
        this.key = key;
        this.volume = Math.max(0f, Math.min(1f, volume));
    }
}

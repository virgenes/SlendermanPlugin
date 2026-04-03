package me.dreamdevs.slender.music;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parses NBS (Note Block Studio) files into {@link NbsSong} objects.
 * Supports both the classic format (pre-v5) and the Open NBS format (v5+).
 *
 * NBS format reference: https://opennbs.org/nbs
 *
 * All values are little-endian.
 */
public class NbsParser {

    private NbsParser() {}

    /**
     * Parse an NBS file. Returns null on failure.
     */
    public static NbsSong parse(File file, Logger log) {
        try (InputStream is = new FileInputStream(file)) {
            return parse(is, log);
        } catch (IOException e) {
            if (log != null) log.warning("[NbsParser] Could not open " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static NbsSong parse(InputStream is, Logger log) throws IOException {
        // ---- Header ----
        int version = 0;
        int songLength = 0;

        short firstShort = readShort(is);

        if (firstShort == 0) {
            // New Open NBS format (version 5+)
            version = readUByte(is);         // NBS version
            readUByte(is);                   // vanilla instrument count (ignore)
            if (version >= 3) {
                songLength = readUShort(is); // song length in ticks
            }
        } else {
            // Classic format — firstShort is the song length
            songLength = firstShort & 0xFFFF;
        }

        /* int layerCount = */ readUShort(is);  // layer count (not needed)

        readString(is); // song name
        readString(is); // song author
        readString(is); // original author
        readString(is); // description

        int tempoRaw = readUShort(is); // tempo = ticks/sec * 100
        float tempo = tempoRaw / 100.0f;

        readUByte(is);  // auto save enabled
        readUByte(is);  // auto save duration
        readUByte(is);  // time signature
        readInt(is);    // minutes spent
        readInt(is);    // left clicks
        readInt(is);    // right clicks
        readInt(is);    // note blocks added
        readInt(is);    // note blocks removed
        readString(is); // imported midi/schematic name

        if (version >= 4) {
            readUByte(is);  // loop on completion
            readUByte(is);  // max loop count
            readUShort(is); // loop start tick
        }

        // ---- Notes ----
        Map<Integer, List<NbsNote>> notesByTick = new HashMap<>();
        int currentTick = -1;
        int maxTick = 0;

        while (true) {
            int tickJump = readUShort(is);
            if (tickJump == 0) break;
            currentTick += tickJump;
            if (currentTick > maxTick) maxTick = currentTick;

            while (true) {
                int layerJump = readUShort(is);
                if (layerJump == 0) break;

                int instrument = readUByte(is);
                int key        = readUByte(is);
                float volume   = 1.0f;

                if (version >= 4) {
                    int vel = readUByte(is);       // velocity 0-100
                    volume = vel / 100.0f;
                    readUByte(is);                  // panning (100 = centre)
                    readShort(is);                  // fine tuning pitch (semitones * 100) — ignore
                }

                notesByTick.computeIfAbsent(currentTick, k -> new ArrayList<>())
                           .add(new NbsNote(instrument, key, volume));
            }
        }

        // For classic format without explicit length, derive from notes
        if (version < 3 || songLength == 0) {
            songLength = maxTick;
        }

        return new NbsSong(tempo, songLength, notesByTick);
    }

    // ---- Little-endian readers ----

    private static short readShort(InputStream is) throws IOException {
        int b0 = checkedRead(is);
        int b1 = checkedRead(is);
        return (short) (b0 | (b1 << 8));
    }

    private static int readUShort(InputStream is) throws IOException {
        int b0 = checkedRead(is);
        int b1 = checkedRead(is);
        return (b0 | (b1 << 8)) & 0xFFFF;
    }

    private static int readInt(InputStream is) throws IOException {
        int b0 = checkedRead(is);
        int b1 = checkedRead(is);
        int b2 = checkedRead(is);
        int b3 = checkedRead(is);
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private static int readUByte(InputStream is) throws IOException {
        return checkedRead(is) & 0xFF;
    }

    private static String readString(InputStream is) throws IOException {
        int len = readInt(is);
        if (len <= 0) return "";
        byte[] bytes = new byte[len];
        int totalRead = 0;
        while (totalRead < len) {
            int r = is.read(bytes, totalRead, len - totalRead);
            if (r == -1) break;
            totalRead += r;
        }
        return new String(bytes, 0, totalRead, StandardCharsets.UTF_8);
    }

    private static int checkedRead(InputStream is) throws IOException {
        int val = is.read();
        if (val == -1) throw new IOException("Unexpected end of NBS file");
        return val;
    }
}

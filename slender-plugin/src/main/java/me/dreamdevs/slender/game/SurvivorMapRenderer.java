package me.dreamdevs.slender.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapCursor;

import java.util.List;

public class SurvivorMapRenderer extends MapRenderer {

    private final Arena arena;

    public SurvivorMapRenderer(Arena arena) {
        super(true); // Contextual
        this.arena = arena;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        MapCursorCollection cursors = canvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        // Draw page locations FIRST (so they are drawn underneath)
        List<Location> pages = arena.getPagesLocations();
        for (Location loc : pages) {
            if (loc.getWorld().equals(map.getWorld())) {
                // Use a red marker for pages
                cursors.addCursor(calculateCursor(map, loc, MapCursor.Type.RED_X, false));
            }
        }

        // Draw player cursor LAST (so it is on top)
        Location playerLoc = player.getLocation();
        if (playerLoc.getWorld().equals(map.getWorld())) {
            cursors.addCursor(calculateCursor(map, playerLoc, MapCursor.Type.PLAYER, true));
        }
    }

    private MapCursor calculateCursor(MapView map, Location loc, MapCursor.Type type, boolean showDirection) {
        int centerX = map.getCenterX();
        int centerZ = map.getCenterZ();
        int scale = 1 << map.getScale().getValue();

        int x = (loc.getBlockX() - centerX) / scale;
        int z = (loc.getBlockZ() - centerZ) / scale;

        // Clamp to map bounds (-128 to 127)
        byte mapX = (byte) Math.max(-128, Math.min(127, x));
        byte mapZ = (byte) Math.max(-128, Math.min(127, z));

        byte direction = 0;
        if (showDirection) {
            float yaw = loc.getYaw();
            if (yaw < 0) yaw += 360;
            direction = (byte) ((yaw * 16 / 360) % 16);
        }

        return new MapCursor(mapX, mapZ, direction, type, true);
    }
}

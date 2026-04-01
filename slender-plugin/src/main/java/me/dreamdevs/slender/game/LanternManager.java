package me.dreamdevs.slender.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manages lantern items given to survivors at game start.
 */
public class LanternManager {

    private final Arena arena;

    public LanternManager(Arena arena) {
        this.arena = arena;
    }

    public void start() {
        // Lanterns are given in sendPlayersToGame via giveLantern()
    }

    public void stop() {
        // Remove lanterns from survivors on game end
        for (Player player : arena.getPlayers().keySet()) {
            if (player.isOnline()) {
                player.getInventory().setItem(2, null);
            }
        }
    }

    /**
     * Gives a lantern item to a player in the specified slot.
     *
     * @param player the player to give the lantern to
     * @param slot   the inventory slot (2 for survivors)
     */
    public void giveLantern(Player player, int slot) {
        ItemStack lantern = new ItemStack(Material.LANTERN);
        ItemMeta meta = lantern.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Lantern", NamedTextColor.GOLD));
            lantern.setItemMeta(meta);
        }
        player.getInventory().setItem(slot, lantern);
    }
}

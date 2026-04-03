package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.FlashlightManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class FlashlightListener implements Listener {

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gamePlayer == null || !gamePlayer.isInArena()) return;

        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null || arena.getArenaState() != ArenaState.RUNNING) return;

        if (arena.getPlayers().get(player) != Role.SURVIVOR) return;

        FlashlightManager fm = arena.getFlashlightManager();
        if (fm != null && fm.isActive(player)) {
            // Turning off flashlight by changing slot
            fm.toggle(player);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gamePlayer == null || !gamePlayer.isInArena()) return;

        Arena arena = (Arena) gamePlayer.getArena();
        if (arena == null || arena.getArenaState() != ArenaState.RUNNING) return;

        if (arena.getPlayers().get(player) != Role.SURVIVOR) return;

        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped == null || !dropped.hasItemMeta()) return;

        ItemMeta meta = dropped.getItemMeta();
        if (meta.getPersistentDataContainer().has(FlashlightManager.BATTERY_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true); // Disable dropping the flashlight completely!
            
            FlashlightManager fm = arena.getFlashlightManager();
            if (fm != null && fm.isActive(player)) {
                fm.toggle(player);
            }
        }
    }
}

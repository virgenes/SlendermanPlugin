package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.GameMode;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class VoteModeMenu extends ItemMenu {

    public VoteModeMenu(Arena arena) {
        super("Vote Game Mode", Size.THREE_LINE);

        if (arena.getAllowedModes().contains(GameMode.CLASSIC)) {
            setItem(11, new MenuItem("&aClassic Mode", new ItemStack(Material.BOOK), 
                    "&7Vote for the Classic Slenderman experience.",
                    "&7Collect 8 pages and survive!") {
                @Override
                public void onItemClick(ItemClickEvent event) {
                    event.setWillClose(true);
                    arena.getModeVotes().put(event.getPlayer().getUniqueId(), GameMode.CLASSIC);
                    event.getPlayer().sendMessage("§aYou voted for Classic Mode!");
                }
            });
        }

        if (arena.getAllowedModes().contains(GameMode.INFECTION)) {
            setItem(15, new MenuItem("&cInfection Mode", new ItemStack(Material.ROTTEN_FLESH), 
                    "&7Vote for 'El Culto de los Proxies'.",
                    "&7Slenderman converts players into Proxies!") {
                @Override
                public void onItemClick(ItemClickEvent event) {
                    event.setWillClose(true);
                    arena.getModeVotes().put(event.getPlayer().getUniqueId(), GameMode.INFECTION);
                    event.getPlayer().sendMessage("§cYou voted for Infection Mode!");
                }
            });
        }
    }
}

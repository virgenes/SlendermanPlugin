package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.listeners.PlayerInteractListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GeneratorMenu extends ItemMenu {

    public GeneratorMenu(Arena arena, Location loc, Player player) {
        super(Langauge.ER_MENU_GENERATOR_TITLE.toString()
                .replace("%PROGRESS%", String.valueOf(arena.getGeneratorProgress().getOrDefault(loc, 0.0).intValue())), 
                Size.THREE_LINE);

        setItem(13, new MenuItem(Langauge.ER_MENU_GENERATOR_REPAIR_BUTTON.toString(), new ItemStack(Material.LEVER), 
            "§7Haz clic para comenzar a reparar",
            "§7este generador.",
            "",
            "§6Progreso: §f" + arena.getGeneratorProgress().getOrDefault(loc, 0.0).intValue() + "%") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                PlayerInteractListener.startRepair(player, arena, loc);
            }
        });
    }
}

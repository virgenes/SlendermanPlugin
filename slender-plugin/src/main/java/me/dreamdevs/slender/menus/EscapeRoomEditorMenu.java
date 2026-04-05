package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.CustomItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class EscapeRoomEditorMenu extends ItemMenu {

    public EscapeRoomEditorMenu(Arena arena) {
        super("Escape Room Editor: " + arena.getId(), Size.THREE_LINE);

        // Slot 10: Architect Toolkit
        setItem(10, new MenuItem(Langauge.MENU_EDITOR_MINIMUM_PLAYERS_ITEM_NAME.toString().replace("%AMOUNT%", ""), new ItemStack(Material.CHEST), 
                "&7Click to receive the Escape Room tools",
                "&7in your inventory.") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                event.getPlayer().getInventory().addItem(CustomItem.ER_GENERATOR_TOOL.toItemStack());
                event.getPlayer().getInventory().addItem(CustomItem.ER_KEY_TOOL.toItemStack());
                event.getPlayer().getInventory().addItem(CustomItem.ER_ESCAPE_TOOL.toItemStack());
                event.getPlayer().getInventory().addItem(CustomItem.ER_SLENDER_TOOL.toItemStack());
                event.getPlayer().getInventory().addItem(CustomItem.ER_SURVIVOR_TOOL.toItemStack());
                event.getPlayer().sendMessage(Langauge.ADMIN_SAVED_ARENA_SETTINGS_SUCCESSFULLY.toString());
            }
        });
        
        // Slot 11: Set Survivor Spawn
        setItem(11, new MenuItem(Langauge.MENU_EDITOR_ADD_SURVIVOR_SPAWN_ITEM_NAME.toString(), new ItemStack(Material.BEACON), "&7Set main spawn point") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                arena.getSurvivorsLocations().add(event.getPlayer().getLocation());
                me.dreamdevs.slender.SlenderMain.getInstance().getGameManager().saveGame(arena);
                event.getPlayer().sendMessage(Langauge.ADMIN_SET_SURVIVORS_SPAWN_SUCCESSFULLY.toString());
                new EscapeRoomEditorMenu(arena).open(event.getPlayer());
            }
        });

        // Slot 13: Code Generation (Center)
        String currentCode = (arena.getEscapeCode() != null ? arena.getEscapeCode() : "---");
        setItem(13, new MenuItem("§b§l" + Langauge.ARENA_TITLE.toString(), new ItemStack(Material.PAPER),
            Langauge.MENU_EDITOR_ER_GENERATE_CODE_ITEM_LORE.toString().replace("%CODE%", currentCode).split("\n")) {
            @Override
            public void onItemClick(ItemClickEvent event) {
                int code = (int) (Math.random() * 9000) + 1000;
                arena.setEscapeCode(String.valueOf(code));
                me.dreamdevs.slender.SlenderMain.getInstance().getGameManager().saveGame(arena);
                
                String msg = Langauge.ER_CODE_GENERATED.toString().replace("%CODE%", String.valueOf(code));
                event.getPlayer().sendMessage(msg);
                new EscapeRoomEditorMenu(arena).open(event.getPlayer());
            }
        });
        
        // Slot 15: Set Max Players
        setItem(15, new MenuItem(Langauge.MENU_EDITOR_MAXIMUM_PLAYERS_ITEM_NAME.toString().replace("%AMOUNT%", String.valueOf(arena.getMaxPlayers())), new ItemStack(Material.LAPIS_LAZULI), "&7Left/Right click to change") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                arena.setMaxPlayers(event.getClicktype().isLeftClick() ? arena.getMaxPlayers()+1 : arena.getMaxPlayers()-1);
                me.dreamdevs.slender.SlenderMain.getInstance().getGameManager().saveGame(arena);
                new EscapeRoomEditorMenu(arena).open(event.getPlayer());
            }
        });
        
        // Slot 16: Save Arena
        setItem(16, new MenuItem(Langauge.MENU_EDITOR_SAVE_SETTINGS_ITEM_NAME.toString(), new ItemStack(Material.CLOCK), "&7Save changes") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                me.dreamdevs.slender.SlenderMain.getInstance().getGameManager().saveGame(arena);
                event.getPlayer().sendMessage(Langauge.ADMIN_SAVED_ARENA_SETTINGS_SUCCESSFULLY.toString());
            }
        });
    }
}

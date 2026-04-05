package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.api.game.Role;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KeypadMenu extends ItemMenu {

    private final Arena arena;
    private String currentInput = "";

    public KeypadMenu(Arena arena) {
        super("&8Keypad: Enter Code", Size.FIVE_LINE);
        this.arena = arena;
        updateButtons();
    }

    private void updateButtons() {
        clearAllItems();
        
        for (int i = 0; i < 9; i++) {
            final int num = i + 1;
            setItem(10 + (i % 3) + ((i / 3) * 9), new MenuItem("&f&l" + num, new ItemStack(Material.WHITE_STAINED_GLASS_PANE)) {
                @Override
                public void onItemClick(me.dreamdevs.slender.api.events.ItemClickEvent event) {
                    if (currentInput.length() < 4) {
                        currentInput += num;
                        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                        updateButtons();
                        update(event.getPlayer());
                    }
                }
            });
        }

        // 0 key
        setItem(37, new MenuItem("&f&l0", new ItemStack(Material.WHITE_STAINED_GLASS_PANE)) {
            @Override
            public void onItemClick(me.dreamdevs.slender.api.events.ItemClickEvent event) {
                if (currentInput.length() < 4) {
                    currentInput += "0";
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                    updateButtons();
                    update(event.getPlayer());
                }
            }
        });

        // Clear key
        setItem(36, new MenuItem("&cClear", new ItemStack(Material.RED_STAINED_GLASS_PANE)) {
            @Override
            public void onItemClick(me.dreamdevs.slender.api.events.ItemClickEvent event) {
                currentInput = "";
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                updateButtons();
                update(event.getPlayer());
            }
        });

        // Enter key
        setItem(38, new MenuItem("&aEnter", new ItemStack(Material.LIME_STAINED_GLASS_PANE)) {
            @Override
            public void onItemClick(me.dreamdevs.slender.api.events.ItemClickEvent event) {
                Player p = event.getPlayer();
                if (currentInput.equals(arena.getEscapeCode())) {
                    p.sendMessage(Langauge.ER_KEYPAD_ACCESS_GRANTED.toString());
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1f, 1f);
                    p.closeInventory();
                    arena.endGame(Role.SURVIVOR);
                } else {
                    p.sendMessage(Langauge.ER_KEYPAD_INVALID_CODE.toString());
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    currentInput = "";
                    updateButtons();
                    update(p);
                }
            }
        });

        // Display
        setItem(4, new MenuItem("&fInput: &e" + currentInput + "_", new ItemStack(Material.BLACK_STAINED_GLASS_PANE)));
    }
}

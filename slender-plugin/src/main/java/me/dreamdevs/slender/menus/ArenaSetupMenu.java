package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.ArenaType;
import me.dreamdevs.slender.api.game.GameMode;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ArenaSetupMenu extends ItemMenu {

    public ArenaSetupMenu(Arena arena) {
        super("Arena Setup: " + arena.getId(), Size.THREE_LINE);

        setItem(11, new SetupSetting(arena, SetupSetting.Action.SET_STANDARD,
                "&aSet as Standard Arena",
                new ItemStack(Material.COMPASS),
                "&7Click to set this arena to STANDARD type.",
                "&7(Players will vote for Classic/Infection in Lobby)"));

        setItem(13, new SetupSetting(arena, SetupSetting.Action.SET_ESCAPE_ROOM,
                "&bSet as Escape Room",
                new ItemStack(Material.IRON_DOOR),
                "&7Click to set this arena to ESCAPE ROOM type.",
                "&7(No voting, uses Architect Toolset in Edit mode)"));

        setItem(15, new SetupSetting(arena, SetupSetting.Action.TOGGLE_MODES,
                "&dToggle Allowed Modes",
                new ItemStack(Material.REPEATER),
                "&7Click to toggle allowed modes for a Standard Arena.",
                "&7Currently: &e" + arena.getAllowedModes().toString()));
    }

    private static class SetupSetting extends MenuItem {

        public enum Action {
            SET_STANDARD, SET_ESCAPE_ROOM, TOGGLE_MODES
        }

        private final Arena arena;
        private final Action action;

        public SetupSetting(Arena arena, Action action, String displayName, ItemStack icon, String... lore) {
            super(displayName, icon, lore);
            this.arena = arena;
            this.action = action;
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            switch (action) {
                case SET_STANDARD:
                    arena.setArenaType(ArenaType.STANDARD);
                    event.getPlayer().sendMessage("§aArena successfully set to STANDARD mode.");
                    SlenderMain.getInstance().getGameManager().saveGame(arena);
                    break;
                case SET_ESCAPE_ROOM:
                    arena.setArenaType(ArenaType.ESCAPE_ROOM);
                    event.getPlayer().sendMessage("§aArena successfully set to ESCAPE ROOM mode.");
                    SlenderMain.getInstance().getGameManager().saveGame(arena);
                    break;
                case TOGGLE_MODES:
                    if (arena.getArenaType() == ArenaType.ESCAPE_ROOM) {
                        event.getPlayer().sendMessage("§cCannot toggle standard modes while in Escape Room type.");
                    } else {
                        if (arena.getAllowedModes().contains(GameMode.CLASSIC) && arena.getAllowedModes().contains(GameMode.INFECTION)) {
                            arena.getAllowedModes().remove(GameMode.INFECTION);
                        } else if (arena.getAllowedModes().contains(GameMode.CLASSIC)) {
                            arena.getAllowedModes().remove(GameMode.CLASSIC);
                            arena.getAllowedModes().add(GameMode.INFECTION);
                        } else {
                            arena.getAllowedModes().add(GameMode.CLASSIC);
                        }
                        SlenderMain.getInstance().getGameManager().saveGame(arena);
                    }
                    new ArenaSetupMenu(arena).open(event.getPlayer());
                    break;
            }
        }
    }
}

package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class EditorMenu extends ItemMenu {

	public EditorMenu(Arena arena) {
		super(Langauge.MENU_EDITOR_TITLE.toString(), Size.THREE_LINE);

		setItem(10, new SetSetting(arena, SetSetting.ArenaSetting.MINIMUM_PLAYERS,
				Langauge.MENU_EDITOR_MINIMUM_PLAYERS_ITEM_NAME.toString().replace("%AMOUNT%", String.valueOf(arena.getMinPlayers())),
				new ItemStack(Material.REDSTONE),
				Langauge.MENU_EDITOR_MINIMUM_PLAYERS_ITEM_LORE.toString().replace("%AMOUNT%", String.valueOf(arena.getMinPlayers()))));

		setItem(11, new SetSetting(arena, SetSetting.ArenaSetting.MAXIMUM_PLAYERS,
				Langauge.MENU_EDITOR_MAXIMUM_PLAYERS_ITEM_NAME.toString().replace("%AMOUNT%", String.valueOf(arena.getMaxPlayers())),
				new ItemStack(Material.LAPIS_LAZULI),
				Langauge.MENU_EDITOR_MAXIMUM_PLAYERS_ITEM_LORE.toString().replace("%AMOUNT%", String.valueOf(arena.getMaxPlayers()))));

		setItem(12, new SetSetting(arena, SetSetting.ArenaSetting.SLENDERMAN_SPAWN,
				Langauge.MENU_EDITOR_SET_SLENDERMAN_SPAWN_ITEM_NAME.toString(),
				new ItemStack(Material.REDSTONE_BLOCK),
				Langauge.MENU_EDITOR_SET_SLENDERMAN_SPAWN_ITEM_LORE.toString()));

		setItem(13, new SetSetting(arena, SetSetting.ArenaSetting.SURVIVORS_SPAWN,
				Langauge.MENU_EDITOR_ADD_SURVIVOR_SPAWN_ITEM_NAME.toString(),
				new ItemStack(Material.BEACON),
				Langauge.MENU_EDITOR_ADD_SURVIVOR_SPAWN_ITEM_LORE.toString()));

		setItem(14, new SetSetting(arena, SetSetting.ArenaSetting.PAGES_SPAWN,
				Langauge.MENU_EDITOR_ADD_PAGES_SPAWN_ITEM_NAME.toString(),
				new ItemStack(Material.BEACON),
				Langauge.MENU_EDITOR_ADD_PAGES_SPAWN_ITEM_LORE.toString()));

		setItem(15, new SetSetting(arena, SetSetting.ArenaSetting.GAME_TIME,
				Langauge.MENU_EDITOR_GAME_TIME_ITEM_NAME.toString().replace("%AMOUNT%", String.valueOf(arena.getGameTime())),
				new ItemStack(Material.CLOCK),
				Langauge.MENU_EDITOR_GAME_TIME_ITEM_LORE.toString().replace("%AMOUNT%", String.valueOf(arena.getGameTime()))));

		setItem(16, new SetSetting(arena, SetSetting.ArenaSetting.SAVE,
				Langauge.MENU_EDITOR_SAVE_SETTINGS_ITEM_NAME.toString(),
				new ItemStack(Material.CLOCK),
				Langauge.MENU_EDITOR_SAVE_SETTINGS_ITEM_LORE.toString()));
	}

	private static class SetSetting extends MenuItem {

		private enum ArenaSetting {
			MINIMUM_PLAYERS, MAXIMUM_PLAYERS, SLENDERMAN_SPAWN, GAME_TIME, SURVIVORS_SPAWN, PAGES_SPAWN, SAVE
		}

		private final Arena arena;
		private final ArenaSetting arenaSetting;

		public SetSetting(Arena arena, ArenaSetting arenaSetting, String displayName, ItemStack icon, String... lore) {
			super(displayName, icon, lore);
			this.arena = arena;
			this.arenaSetting = arenaSetting;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			switch (arenaSetting) {
				case MINIMUM_PLAYERS:
					arena.setMinPlayers(event.getClicktype().isLeftClick() ? arena.getMinPlayers()+1 : arena.getMinPlayers()-1);
					new EditorMenu(arena).open(event.getPlayer());
					break;
				case MAXIMUM_PLAYERS:
					arena.setMaxPlayers(event.getClicktype().isLeftClick() ? arena.getMaxPlayers()+1 : arena.getMaxPlayers()-1);
					new EditorMenu(arena).open(event.getPlayer());
					break;
			case PAGES_SPAWN:
				arena.getPagesLocations().add(event.getPlayer().getLocation());
				event.getPlayer().sendMessage(Langauge.ADMIN_SET_PAGES_SPAWN_SUCCESSFULLY.toString());
				new EditorMenu(arena).open(event.getPlayer());
				break;
			case GAME_TIME:
				arena.setGameTime(event.getClicktype().isLeftClick() ? arena.getGameTime()+1 : arena.getGameTime()-1);
				new EditorMenu(arena).open(event.getPlayer());
				break;
			case SURVIVORS_SPAWN:
				arena.getSurvivorsLocations().add(event.getPlayer().getLocation());
				event.getPlayer().sendMessage(Langauge.ADMIN_SET_SURVIVORS_SPAWN_SUCCESSFULLY.toString());
				new EditorMenu(arena).open(event.getPlayer());
				break;
			case SLENDERMAN_SPAWN:
				arena.setSlenderManSpawnLocation(event.getPlayer().getLocation());
				event.getPlayer().sendMessage(Langauge.ADMIN_SET_SLENDERMAN_SPAWN_SUCCESSFULLY.toString());
				new EditorMenu(arena).open(event.getPlayer());
				break;
				case SAVE:
					SlenderMain.getInstance().getGameManager().saveGame(arena);
					event.getPlayer().sendMessage(Langauge.ADMIN_SAVED_ARENA_SETTINGS_SUCCESSFULLY.toString());
					break;
			}
		}
	}

}
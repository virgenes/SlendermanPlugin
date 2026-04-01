package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SelectArenaMenu extends ItemMenu {

	public SelectArenaMenu() {
		super(Langauge.MENU_ARENA_SELECTOR_TITLE.toString(), Size.THREE_LINE);

		List<Arena> arenas = SlenderMain.getInstance().getGameManager().getArenas();
		int slot = 10;
		for (Arena arena : arenas) {
			setItem(slot, new SelectArenaItem(arena));
			slot++;
			if (slot == 17) slot = 19;
		}
	}

	private static class SelectArenaItem extends MenuItem {

		private final Arena arena;

		public SelectArenaItem(Arena arena) {
			super(ColourUtil.colorize("&aArena: &b" + arena.getId()),
					new ItemStack(Material.GRASS_BLOCK),
					buildLore(arena).toArray(String[]::new));
			this.arena = arena;
		}

		private static java.util.List<String> buildLore(Arena arena) {
			String status;
			switch (arena.getArenaState()) {
				case WAITING: status = Langauge.ARENA_STATUS_WAITING.toString(); break;
				case STARTING: status = Langauge.ARENA_STATUS_STARTING.toString(); break;
				case RUNNING: status = Langauge.ARENA_STATUS_RUNNING.toString(); break;
				case ENDING: status = Langauge.ARENA_STATUS_ENDING.toString(); break;
				case RESTARTING: status = Langauge.ARENA_STATUS_RESTARTING.toString(); break;
				default: status = "Unknown"; break;
			}
			return ColourUtil.colouredLore(java.util.Arrays.asList(
					"",
					"&7Players: &b" + arena.getPlayers().size() + "&7/&a" + arena.getMaxPlayers(),
					"&7Status: " + status,
					"",
					"&eClick to join this arena"
			));
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			SlenderMain.getInstance().getGameManager().joinGame(event.getPlayer(), arena);
		}
	}
}
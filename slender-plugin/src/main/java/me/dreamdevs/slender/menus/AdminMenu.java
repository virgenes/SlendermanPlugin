package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.Lobby;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AdminMenu extends ItemMenu {

	public AdminMenu(GamePlayer gamePlayer) {
		super(Langauge.MENU_ADMIN_MENU_TITLE.toString(), Size.THREE_LINE);

		if (gamePlayer.isInArena()) {
			setItem(12, new TakeActionItem(gamePlayer, Langauge.MENU_ADMIN_MENU_FORCE_START_GAME_ITEM.toString(), new ItemStack(Material.EMERALD), TakeActionItem.Action.START));
			setItem(13, new TakeActionItem(gamePlayer, Langauge.MENU_ADMIN_MENU_FORCE_STOP_GAME_ITEM.toString(), new ItemStack(Material.REDSTONE), TakeActionItem.Action.STOP));
			setItem(14, new TakeActionItem(gamePlayer, Langauge.MENU_ADMIN_MENU_FORCE_RESTART_GAME_ITEM.toString(), new ItemStack(Material.SLIME_BALL), TakeActionItem.Action.RESTART));
		} else {
			setItem(13, new TakeActionItem(gamePlayer, Langauge.MENU_ADMIN_MENU_FORCE_SET_LOBBY_ITEM.toString(), new ItemStack(Material.BEACON), TakeActionItem.Action.SET_LOBBY));
		}
	}

	private static class TakeActionItem extends MenuItem {

		private enum Action {
			START,STOP,RESTART,SET_LOBBY;
		}

		private final GamePlayer gamePlayer;
		private final Action action;

		public TakeActionItem(GamePlayer gamePlayer, String displayName, ItemStack itemStack, Action action) {
			super(displayName, itemStack);
			this.gamePlayer = gamePlayer;
			this.action = action;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			Arena arena = (Arena) gamePlayer.getArena();
			switch (action) {
				case START:
					if(arena.isRunning()) {
						gamePlayer.getPlayer().sendMessage(Langauge.ADMIN_FORCE_START_ARENA_UNSUCCESSFULLY.toString());
						return;
					}
					arena.start();
					gamePlayer.getPlayer().sendMessage(Langauge.ADMIN_FORCE_START_ARENA_SUCCESSFULLY.toString());
					break;
				case STOP:
					if(!arena.isRunning()) {
						gamePlayer.getPlayer().sendMessage(Langauge.ADMIN_FORCE_STOP_ARENA_UNSUCCESSFULLY.toString());
						return;
					}
					arena.endGame(me.dreamdevs.slender.api.game.Role.SLENDER);
					gamePlayer.getPlayer().sendMessage(Langauge.ADMIN_FORCE_STOP_ARENA_SUCCESSFULLY.toString());
					break;
				case RESTART:
					arena.restart();
					gamePlayer.getPlayer().sendMessage(Langauge.ADMIN_FORCE_RESTART_ARENA_SUCCESSFULLY.toString());
					break;
				case SET_LOBBY:
					Lobby lobby = SlenderMain.getInstance().getLobby();
					lobby.saveLobby(event.getPlayer());
					gamePlayer.getPlayer().sendMessage(Langauge.ADMIN_SET_LOBBY_SUCCESSFULLY.toString());
					break;
			}
		}
	}

}
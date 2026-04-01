package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.party.PartyRole;
import me.dreamdevs.slender.api.game.party.PartySettings;
import me.dreamdevs.slender.api.inventory.BookItemMenu;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Party;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class PartyMenu extends ItemMenu {

	public PartyMenu(GamePlayer gamePlayer) {
		super(Langauge.MENU_PARTY_TITLE.toString(), Size.THREE_LINE);

		if (SlenderMain.getInstance().getPartyManager().isInParty(gamePlayer)) {
			Party party = SlenderMain.getInstance().getPartyManager().getParty(gamePlayer);

			setItem(4, new PartyButton(gamePlayer, PartyButton.Action.NOTHING, party, Langauge.MENU_PARTY_INFO_ITEM_NAME.toString(),
						new ItemStack(Material.BOOK), Langauge.MENU_PARTY_INFO_ITEM_LORE.toString()
								.replace("%LEADER%", party.getPartyLeader().getPlayer().getName())
								.replace("%MEMBERS_COUNT%", String.valueOf(party.getMembersMap().size()))
								.replace("%STATUS%", (boolean) party.getPartySetting(PartySettings.OPEN_PARTY) ? Langauge.MENU_PARTY_PUBLIC.toString() : Langauge.MENU_PARTY_PRIVATE.toString())));

			if (!party.getPartyLeader().equals(gamePlayer.getPlayer())) {
				return;
			}

			setItem(12, new PartyButton(gamePlayer, PartyButton.Action.DELETE, party, Langauge.MENU_PARTY_DELETE_ITEM_NAME.toString(),
						new ItemStack(Material.COAL), Langauge.MENU_PARTY_DELETE_ITEM_LORE.toString()));

			setItem(14, new PartyButton(gamePlayer, PartyButton.Action.CHANGE_STATUS, party, Langauge.MENU_PARTY_CHANGE_STATUS_ITEM_NAME.toString(),
						new ItemStack(Material.ARROW), Langauge.MENU_PARTY_CHANGE_STATUS_ITEM_LORE.toString()));
		} else {
			setItem(12, new PartyButton(gamePlayer, PartyButton.Action.FIND, null, Langauge.MENU_PARTY_FIND_PARTY_ITEM_LORE.toString(),
						new ItemStack(Material.SLIME_BALL), Langauge.MENU_PARTY_FIND_PARTY_ITEM_LORE.toString()));

			setItem(14, new PartyButton(gamePlayer, PartyButton.Action.CREATE, null, Langauge.MENU_PARTY_CREATE_ITEM_NAME.toString(),
						new ItemStack(Material.ANVIL), Langauge.MENU_PARTY_CREATE_ITEM_LORE.toString()));
		}
	}

	private static class PartyButton extends MenuItem {

		private enum Action {
			NOTHING,
			CREATE,
			DELETE,
			CHANGE_STATUS,
			INVITE,
			FIND;
		}

		private final GamePlayer gamePlayer;
		private final Action action;
		private final Party party;

		public PartyButton(GamePlayer gamePlayer, Action action, Party party, String displayName, ItemStack icon, String... lore) {
			super(displayName, icon, lore);
			this.gamePlayer = gamePlayer;
			this.action = action;
			this.party = party;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			switch (action) {
				case NOTHING:
					break;
				case DELETE:
					event.getPlayer().sendMessage(Langauge.PARTY_REMOVED_INFO.toString());
					SlenderMain.getInstance().getPartyManager().getParties().remove(party);
					break;
				case CHANGE_STATUS:
					party.setPartySetting(PartySettings.OPEN_PARTY, !(boolean) party.getPartySetting(PartySettings.OPEN_PARTY));
					String status = ((boolean) party.getPartySetting(PartySettings.OPEN_PARTY))
							? Langauge.MENU_PARTY_PUBLIC.toString()
							: Langauge.MENU_PARTY_PRIVATE.toString();
					event.getPlayer().sendMessage(Langauge.PARTY_CHANGED_STATUS.toString()
							.replace("%STATUS%", status)
							.replace("%LEADER%", event.getPlayer().getName()));
					break;
				case CREATE:
					Party party = new Party();
					party.getMembersMap().putIfAbsent(gamePlayer, PartyRole.LEADER);
					SlenderMain.getInstance().getPartyManager().getParties().add(party);
					event.getPlayer().closeInventory();

					new PartyMenu(gamePlayer).open(gamePlayer.getPlayer());
					break;
				case FIND:
					Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
						new PartyList().open(event.getPlayer());
					},4L);
					break;
				case INVITE:
					Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
						new PartyInvite(SlenderMain.getInstance().getPartyManager().getParty(gamePlayer)).open(event.getPlayer());
					},4L);
					break;
			}
		}
	}

	private static class PartyList extends BookItemMenu {

		public PartyList() {
			super(Langauge.MENU_PARTY_TITLE.toString(), buildItems(), true, false);
		}

		private static List<MenuItem> buildItems() {
			return SlenderMain.getInstance().getPartyManager().getParties().stream()
					.filter(party -> (boolean) party.getPartySetting(PartySettings.OPEN_PARTY))
					.map(PartyItem::new)
					.collect(Collectors.toList());
		}

		private static class PartyItem extends MenuItem {

			private final Party party;

			public PartyItem(Party party) {
				super(ColourUtil.colorize("&bParty: "+party.getPartyLeader().getName()), new ItemStack(Material.MELON_SLICE));
				this.party = party;
			}

			@Override
			public void onItemClick(ItemClickEvent event) {
				event.setWillClose(true);
				SlenderMain.getInstance().getPartyManager().joinParty(event.getPlayer(), party);
			}
		}
	}

	private static class PartyInvite extends BookItemMenu {

		public PartyInvite(Party party) {
			super(Langauge.MENU_PARTY_TITLE.toString(), buildItems(party), true, false);
		}

		private static List<MenuItem> buildItems(Party party) {
			return SlenderMain.getInstance().getPlayerManager().getPlayers().stream()
					.filter(gamePlayer -> !SlenderMain.getInstance().getPartyManager().isInParty(gamePlayer))
					.map(gamePlayer -> new InvitePlayer(gamePlayer.getPlayer(), party))
					.collect(Collectors.toList());
		}

		private static class InvitePlayer extends MenuItem {

			private final Player player;
			private final Party party;

			public InvitePlayer(Player player, Party party) {
				super(player.getName(), new ItemStack(Material.PLAYER_HEAD));
				this.player = player;
				this.party = party;
			}

			@Override
			public void onItemClick(ItemClickEvent event) {
				event.setWillClose(true);
				SlenderMain.getInstance().getPartyManager().joinParty(player, party);
			}
		}

	}

}

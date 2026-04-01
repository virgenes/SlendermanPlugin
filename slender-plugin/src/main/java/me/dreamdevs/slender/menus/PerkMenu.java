package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PerkMenu extends ItemMenu {

	public PerkMenu() {
		super(Langauge.MENU_PERKS_TITLE.toString(), Size.THREE_LINE);

		setItem(12, new OpenPerkSelector(Role.SURVIVOR));
		setItem(14, new OpenPerkSelector(Role.SLENDER));
	}

	private static class OpenPerkSelector extends MenuItem {

		private final Role role;

		public OpenPerkSelector(Role role) {
			super((role == Role.SURVIVOR) ? Langauge.MENU_PERKS_OPEN_SURVIVOR_PERKS.toString()
					: Langauge.MENU_PERKS_OPEN_SLENDERMAN_PERKS.toString(), new ItemStack(
					(role == Role.SURVIVOR) ? Material.PLAYER_HEAD : Material.IRON_SWORD));
			this.role = role;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new PerkSelectorMenu(role).open(event.getPlayer());
			}, 4L);
		}
	}

	private static class PerkSelectorMenu extends ItemMenu {

		public PerkSelectorMenu(Role role) {
			super((role == Role.SURVIVOR ? "&aSurvivor Perks" : "&cSlenderMan Perks"), Size.FOUR_LINE);

			List<Perk> perks = SlenderMain.getInstance().getPerkManager().getPerksByRole(role);
			int slot = 10;
			for (Perk perk : perks) {
				PerkInfo info = perk.getClass().getAnnotation(PerkInfo.class);
				setItem(slot, new SelectPerk(role, perk, info));
				slot++;
				if (slot == 17) slot = 19;
				if (slot == 26) slot = 28;
				if (slot == 35) slot = 37;
			}

			setItem(35, new BackItem());
		}
	}

	private static class SelectPerk extends MenuItem {

		private final Perk perk;
		private final Role role;

		public SelectPerk(Role role, Perk perk, PerkInfo info) {
			super(info.name(), new ItemStack(info.icon()), perk.getLore().toArray(String[]::new));
			this.perk = perk;
			this.role = role;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
			if (gamePlayer != null) {
				gamePlayer.setPerk(role, perk);
			}
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
			event.getPlayer().sendMessage(Langauge.PERKS_SELECTED.toString()
					.replace("%PERK_NAME%", perk.getClass().getAnnotation(PerkInfo.class).name())
					.replace("%TEAM%", (role == Role.SURVIVOR) ? Langauge.ARENA_SURVIVOR_TEAM.toString() : Langauge.ARENA_SLENDERMAN_TEAM.toString()));
		}
	}

	private static class BackItem extends MenuItem {

		public BackItem() {
			super(Langauge.MENU_BACK_ITEM_NAME.toString(), new ItemStack(Material.BARRIER));
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new PerkMenu().open(event.getPlayer());
			}, 4L);
		}
	}

}

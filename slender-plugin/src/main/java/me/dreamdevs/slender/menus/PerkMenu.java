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
import org.bukkit.entity.Player;
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
		private final String perkName;

		public SelectPerk(Role role, Perk perk, PerkInfo info) {
			super(info.name(), new ItemStack(info.icon()));
			this.perk = perk;
			this.role = role;
			this.perkName = info.name();
		}

		@Override
		public ItemStack getFinalIcon(org.bukkit.entity.Player player) {
			GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
			ItemStack item = super.getIcon().clone();
			org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
			java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();

			// Header
			lore.add(net.kyori.adventure.text.Component.text(""));
			
			// Original Lore
			for (String line : perk.getLore()) {
				lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent(line));
			}

			lore.add(net.kyori.adventure.text.Component.text(""));

			if (gp != null) {
				if (gp.getPerk(role) != null && gp.getPerk(role).getClass().equals(perk.getClass())) {
					lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&b&l[EQUIPPED]"));
				} else if (gp.ownsPerk(perkName)) {
					lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&a&l[OWNED]"));
					lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&7Click to select"));
				} else {
					int price = getPrice(perkName);
					lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&6Price: " + price + " coins"));
					lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&7Click to purchase"));
				}
			}

			meta.lore(lore);
			item.setItemMeta(meta);
			return item;
		}

		private int getPrice(String name) {
			try {
				me.dreamdevs.slender.api.Config config = me.dreamdevs.slender.api.Config.valueOf("PRICE_" + name.toUpperCase().replace(" ", "_"));
				return config.toInt();
			} catch (Exception e) {
				return 1000; // Default price
			}
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			Player player = event.getPlayer();
			GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
			if (gp == null) return;

			if (gp.getPerk(role) != null && gp.getPerk(role).getClass().equals(perk.getClass())) {
				player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorize("&cThis perk is already equipped!"));
				return;
			}

			if (gp.ownsPerk(perkName)) {
				// Select
				gp.setPerk(role, perk);
				player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
				player.sendMessage(Langauge.PERKS_SELECTED.toString()
						.replace("%PERK_NAME%", perkName)
						.replace("%TEAM%", (role == Role.SURVIVOR) ? Langauge.ARENA_SURVIVOR_TEAM.toString() : Langauge.ARENA_SLENDERMAN_TEAM.toString()));
				event.setWillUpdate(true);
			} else {
				// Purchase
				int price = getPrice(perkName);
				int balance = gp.getStatistic(me.dreamdevs.slender.api.Statistic.COINS);

				if (balance >= price) {
					gp.setStatistic(me.dreamdevs.slender.api.Statistic.COINS, balance - price);
					gp.unlockPerk(perkName);
					player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
					player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorize("&aYou purchased the perk &e" + perkName + " &afor &6" + price + " coins&a!"));
					event.setWillUpdate(true);
				} else {
					player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
					player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorize("&cYou don't have enough coins! Only (" + balance + "/" + price + ")"));
				}
			}
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

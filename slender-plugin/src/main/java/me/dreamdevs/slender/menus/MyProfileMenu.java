package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MyProfileMenu extends ItemMenu {

	public MyProfileMenu(Player player) {
		super(Langauge.MENU_MY_PROFILE_TITLE.toString(), Size.THREE_LINE);

		GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

		// Decoration
		ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		org.bukkit.inventory.meta.ItemMeta meta = border.getItemMeta();
		meta.displayName(net.kyori.adventure.text.Component.empty());
		border.setItemMeta(meta);
		for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26}) {
			setItem(i, new MenuItem(" ", border) {
				@Override
				public void onItemClick(ItemClickEvent event) {}
			});
		}

		// Statistics
		List<String> list = ColourUtil.colouredLore(Langauge.MENU_MY_PROFILE_STATS_ITEM_LORE.toString()
				.replace("%WINS%", String.valueOf(gamePlayer.getStatistic(Statistic.WINS)))
				.replace("%LEVEL%", String.valueOf(gamePlayer.getStatistic(Statistic.LEVEL)))
				.replace("%EXP%", String.valueOf(gamePlayer.getStatistic(Statistic.EXP)))
				.replace("%COLLECTED_PAGES%", String.valueOf(gamePlayer.getStatistic(Statistic.COLLECTED_PAGES)))
				.replace("%KILLED_SURVIVORS%", String.valueOf(gamePlayer.getStatistic(Statistic.KILLED_SURVIVORS)))
				.replace("%KILLED_SLENDERMEN%", String.valueOf(gamePlayer.getStatistic(Statistic.KILLED_SLENDERMEN)))
				.replace("%TOTAL_KILLS%", String.valueOf((gamePlayer.getStatistic(Statistic.KILLED_SURVIVORS)+gamePlayer.getStatistic(Statistic.KILLED_SLENDERMEN)))));

		setItem(10, new MenuItem(Langauge.MENU_MY_PROFILE_STATS_ITEM_NAME.toString(), new ItemStack(Material.PAPER), list.toArray(String[]::new)));

		// Evolution Features
		setItem(12, new MenuItem(ColourUtil.colorize("&6&lLEVELS &e[Click to View]"), new ItemStack(Material.EXPERIENCE_BOTTLE), 
				ColourUtil.colorize("&7Track your progress and"), ColourUtil.colorize("&7claim your rewards!")) {
			@Override
			public void onItemClick(ItemClickEvent event) {
				new LevelsMenu(gamePlayer).open(event.getPlayer());
			}
		});

		setItem(13, new MenuItem(ColourUtil.colorize("&b&lSKILLS &e[Click to Upgrade]"), new ItemStack(Material.NETHER_STAR), 
				ColourUtil.colorize("&7Upgrade your passive abilities"), ColourUtil.colorize("&7to become unstoppable!")) {
			@Override
			public void onItemClick(ItemClickEvent event) {
				new SkillsMenu(gamePlayer).open(event.getPlayer());
			}
		});

		setItem(14, new MenuItem(ColourUtil.colorize("&e&lACHIEVEMENTS &e[Click to View]"), new ItemStack(Material.BOOK), 
				ColourUtil.colorize("&7Complete challenges and"), ColourUtil.colorize("&7earn badges of honor!")) {
			@Override
			public void onItemClick(ItemClickEvent event) {
				new AchievementsMenu(gamePlayer).open(event.getPlayer());
			}
		});

		// Settings
		setItem(16, new MenuItem(Langauge.MENU_MY_PROFILE_SETTINGS_ITEM_NAME.toString(), new ItemStack(Material.COMPARATOR), 
				ColourUtil.colorize(Langauge.MENU_MY_PROFILE_SETTINGS_ITEM_LORE.toString())) {
			@Override
			public void onItemClick(ItemClickEvent event) {
				new SettingsMenu(gamePlayer).open(event.getPlayer());
			}
		});
	}

}
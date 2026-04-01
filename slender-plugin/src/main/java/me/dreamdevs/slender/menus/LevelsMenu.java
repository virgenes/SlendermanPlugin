package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LevelsMenu extends ItemMenu {

	private static final int LEVELS_PER_PAGE = 28;

	public LevelsMenu(GamePlayer gamePlayer) {
		this(gamePlayer, 0);
	}

	public LevelsMenu(GamePlayer gamePlayer, int page) {
		super(ColourUtil.colorize("&0&lEvolution &8» &6&lLevels &7(Page " + (page + 1) + ")"), Size.SIX_LINE);

		// Border Decoration
		ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		org.bukkit.inventory.meta.ItemMeta meta = border.getItemMeta();
		meta.displayName(net.kyori.adventure.text.Component.empty());
		border.setItemMeta(meta);

		int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
		for (int s : borderSlots) {
			setItem(s, new MenuItem(" ", border) {
				@Override
				public void onItemClick(ItemClickEvent event) {}
			});
		}

		// Player Stats Header
		setItem(4, new MenuItem(ColourUtil.colorize("&e&lYour Progress"), new ItemStack(Material.PLAYER_HEAD), 
				ColourUtil.colorize("&7Current Level: &b" + gamePlayer.getStatistic(Statistic.LEVEL)),
				ColourUtil.colorize("&7Total Experience: &a" + gamePlayer.getStatistic(Statistic.EXP) + " EXP"),
				"",
				ColourUtil.colorize("&eLevel up to unlock new perks and rewards!")) {
			@Override
			public void onItemClick(ItemClickEvent event) {}
		});

		List<Map.Entry<Integer, Level>> allLevels = new ArrayList<>(
				SlenderMain.getInstance().getLevelManager().getLevels().entrySet());
		allLevels.sort(Map.Entry.comparingByKey());

		int start = page * LEVELS_PER_PAGE;
		int end = Math.min(start + LEVELS_PER_PAGE, allLevels.size());

		int[] slots = {
				10, 11, 12, 13, 14, 15, 16,
				19, 20, 21, 22, 23, 24, 25,
				28, 29, 30, 31, 32, 33, 34,
				37, 38, 39, 40, 41, 42, 43
		};

		for (int i = start; i < end; i++) {
			Map.Entry<Integer, Level> entry = allLevels.get(i);
			setItem(slots[i - start], new LevelItem(gamePlayer, entry.getKey(), entry.getValue()));
		}

		int totalPages = (int) Math.ceil((double) allLevels.size() / LEVELS_PER_PAGE);

		// Navigation buttons
		if (page > 0) {
			setItem(45, new PrevPageItem(gamePlayer, page - 1));
		}
		if (page < totalPages - 1) {
			setItem(53, new NextPageItem(gamePlayer, page + 1));
		}

		// Back button
		setItem(49, new BackItem(gamePlayer));
	}

	private static class LevelItem extends MenuItem {

		public LevelItem(GamePlayer gamePlayer, int number, Level level) {
			super(ColourUtil.colorize(getIconName(gamePlayer, number)),
					new ItemStack(getMaterial(gamePlayer, number)),
					getLore(gamePlayer, number, level));
		}

		private static String getIconName(GamePlayer gamePlayer, int number) {
			int current = gamePlayer.getStatistic(Statistic.LEVEL);
			if (current == number) return "&6&lLevel " + number + " &e&l[CURRENT]";
			if (current > number) return "&a&lLevel " + number + " &2&l[UNLOCKED]";
			return "&c&lLevel " + number + " &8&l[LOCKED]";
		}

		private static Material getMaterial(GamePlayer gamePlayer, int number) {
			int current = gamePlayer.getStatistic(Statistic.LEVEL);
			if (current == number) return Material.EXPERIENCE_BOTTLE;
			if (current > number) return Material.EMERALD_BLOCK;
			return Material.COAL;
		}

		private static String[] getLore(GamePlayer gamePlayer, int number, Level level) {
			List<String> lore = new ArrayList<>();
			lore.add(ColourUtil.colorize("&7Required: &e" + level.getRequireExp() + " EXP"));
			lore.add("");
			lore.add(ColourUtil.colorize("&6&lRewards:"));
			if (level.getRewards() != null && !level.getRewards().isEmpty()) {
				for (String r : level.getRewards()) {
					lore.add(ColourUtil.colorize("&8• &f" + r));
				}
			} else {
				lore.add(ColourUtil.colorize("&8• &7No rewards"));
			}
			lore.add("");
			int current = gamePlayer.getStatistic(Statistic.LEVEL);
			if (current == number) {
				lore.add(ColourUtil.colorize("&eKeep playing to reach the next level!"));
			} else if (current > number) {
				lore.add(ColourUtil.colorize("&aYou have already completed this level."));
			} else {
				lore.add(ColourUtil.colorize("&cYou need &f" + (level.getRequireExp() - gamePlayer.getStatistic(Statistic.EXP)) + " more EXP &cto reach this level."));
			}
			return lore.toArray(new String[0]);
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2f);
		}
	}

	private static class PrevPageItem extends MenuItem {
		private final GamePlayer gamePlayer;
		private final int page;

		public PrevPageItem(GamePlayer gamePlayer, int page) {
			super(ColourUtil.colorize("&a&l← Previous Page"), new ItemStack(Material.ARROW));
			this.gamePlayer = gamePlayer;
			this.page = page;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new LevelsMenu(gamePlayer, page).open(event.getPlayer());
			}, 2L);
		}
	}

	private static class NextPageItem extends MenuItem {
		private final GamePlayer gamePlayer;
		private final int page;

		public NextPageItem(GamePlayer gamePlayer, int page) {
			super(ColourUtil.colorize("&a&lNext Page →"), new ItemStack(Material.ARROW));
			this.gamePlayer = gamePlayer;
			this.page = page;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new LevelsMenu(gamePlayer, page).open(event.getPlayer());
			}, 2L);
		}
	}

	private static class BackItem extends MenuItem {

		public BackItem(GamePlayer gamePlayer) {
			super(ColourUtil.colorize("&c&lBack to Profile"), new ItemStack(Material.BARRIER));
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new MyProfileMenu(event.getPlayer()).open(event.getPlayer());
			}, 2L);
		}
	}
}

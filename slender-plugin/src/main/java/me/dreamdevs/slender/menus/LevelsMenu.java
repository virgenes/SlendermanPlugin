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

	private static final int LEVELS_PER_PAGE = 27;

	public LevelsMenu(GamePlayer gamePlayer) {
		this(gamePlayer, 0);
	}

	public LevelsMenu(GamePlayer gamePlayer, int page) {
		super(ColourUtil.colorize("&6&lLevels &7(Page " + (page + 1) + ")"), Size.FIVE_LINE);

		List<Map.Entry<Integer, Level>> allLevels = new ArrayList<>(
				SlenderMain.getInstance().getLevelManager().getLevels().entrySet());
		allLevels.sort(Map.Entry.comparingByKey());

		int start = page * LEVELS_PER_PAGE;
		int end = Math.min(start + LEVELS_PER_PAGE, allLevels.size());

		int slot = 10;
		for (int i = start; i < end; i++) {
			Map.Entry<Integer, Level> entry = allLevels.get(i);
			setItem(slot, new LevelItem(gamePlayer, entry.getKey(), entry.getValue()));
			slot++;
			if (slot == 17) slot = 19;
			if (slot == 26) slot = 28;
		}

		int totalPages = (int) Math.ceil((double) allLevels.size() / LEVELS_PER_PAGE);

		// Navigation buttons
		if (page > 0) {
			setItem(36, new PrevPageItem(gamePlayer, page - 1));
		}
		if (page < totalPages - 1) {
			setItem(44, new NextPageItem(gamePlayer, page + 1));
		}

		// Back button center bottom
		setItem(40, new BackItem(gamePlayer));
	}

	private static class LevelItem extends MenuItem {

		public LevelItem(GamePlayer gamePlayer, int number, Level level) {
			super(ColourUtil.colorize("&bLv. " + number),
					new ItemStack(gamePlayer.getStatistic(Statistic.LEVEL) >= number ? Material.LIME_DYE : Material.GRAY_DYE),
					ColourUtil.colorize("&7Required: &e" + level.getRequireExp() + " EXP"),
					gamePlayer.getStatistic(Statistic.LEVEL) >= number ? ColourUtil.colorize("&a✔ Unlocked") : ColourUtil.colorize("&c✘ Locked"));
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
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
			}, 4L);
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
			}, 4L);
		}
	}

	private static class BackItem extends MenuItem {

		private final GamePlayer gamePlayer;

		public BackItem(GamePlayer gamePlayer) {
			super(ColourUtil.colorize("&c&lBack to Profile"), new ItemStack(Material.BARRIER));
			this.gamePlayer = gamePlayer;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new MyProfileMenu(event.getPlayer()).open(event.getPlayer());
			}, 4L);
		}
	}
}

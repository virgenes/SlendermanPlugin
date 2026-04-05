package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class SettingsMenu extends ItemMenu {

	private static final String[] MESSAGE_TYPES = {"all", "arena", "lobby"};

	public SettingsMenu(GamePlayer gamePlayer) {
		super(Langauge.MENU_MY_PROFILE_SETTINGS_TITLE.toString(), Size.FIVE_LINE);

        // Decoration: Glass panes for a premium look
        for (int i = 0; i < 45; i++) {
            setItem(i, new MenuItem(" ", new ItemStack(Material.GRAY_STAINED_GLASS_PANE)));
        }

		setItem(11, new SetSettingItem(gamePlayer, Setting.AUTO_JOIN_MODE,
				Langauge.MENU_MY_PROFILE_SETTINGS_AUTO_JOIN_MODE_ITEM_NAME.toString(),
				Material.COMPASS,
				ColourUtil.colouredLore(Langauge.MENU_MY_PROFILE_SETTINGS_AUTO_JOIN_MODE_ITEM_LORE.toString()
						.replace("%STATUS%", ((boolean)gamePlayer.getSetting(Setting.AUTO_JOIN_MODE)) ?
											Langauge.MENU_STATUS_ON.toString() : Langauge.MENU_STATUS_OFF.toString())).toArray(String[]::new)));

		setItem(13, new SetSettingItem(gamePlayer, Setting.SHOW_ARENA_JOIN_MESSAGE,
				Langauge.MENU_MY_PROFILE_SETTINGS_SH0W_ARENA_JOIN_MESSAGE_ITEM_NAME.toString(),
				Material.PAPER,
				ColourUtil.colouredLore(Langauge.MENU_MY_PROFILE_SETTINGS_SHOW_ARENA_JOIN_MESSAGE_ITEM_LORE.toString()
						.replace("%STATUS%", ((boolean)gamePlayer.getSetting(Setting.SHOW_ARENA_JOIN_MESSAGE)) ?
								Langauge.MENU_STATUS_ON.toString() : Langauge.MENU_STATUS_OFF.toString())).toArray(String[]::new)));

		setItem(15, new MessageTypeItem(gamePlayer));

		setItem(29, new SetSettingItem(gamePlayer, Setting.MUSIC_ENABLED,
				"&6&lGame Music",
				Material.JUKEBOX,
				ColourUtil.colouredLore(java.util.Arrays.asList(
						"&7Toggle in-game background music.",
						"&7Status: " + ((boolean) gamePlayer.getSetting(Setting.MUSIC_ENABLED) ?
								"&2&lON" : "&4&lOFF")
				)).toArray(String[]::new)));

		setItem(31, new SetSettingItem(gamePlayer, Setting.DARKNESS_FLICKER,
				Langauge.MENU_MY_PROFILE_SETTINGS_DARKNESS_FLICKER_ITEM_NAME.toString(),
				Material.ENDER_EYE,
				ColourUtil.colouredLore(Langauge.MENU_MY_PROFILE_SETTINGS_DARKNESS_FLICKER_ITEM_LORE.toString()
						.replace("%STATUS%", ((boolean)gamePlayer.getSetting(Setting.DARKNESS_FLICKER)) ?
								Langauge.MENU_STATUS_ON.toString() : Langauge.MENU_STATUS_OFF.toString())).toArray(String[]::new)));

		setItem(33, new SetSettingItem(gamePlayer, Setting.FLASHLIGHT_REALISTIC,
				Langauge.MENU_MY_PROFILE_SETTINGS_FLASHLIGHT_STYLE_ITEM_NAME.toString(),
				Material.BEACON,
				ColourUtil.colouredLore(Langauge.MENU_MY_PROFILE_SETTINGS_FLASHLIGHT_STYLE_ITEM_LORE.toString()
						.replace("%STYLE%", ((boolean)gamePlayer.getSetting(Setting.FLASHLIGHT_REALISTIC)) ?
								"Realistic" : "Night Vision")).toArray(String[]::new)));

		setItem(40, new BackToMyProfileItem());
	}

	private static class SetSettingItem extends MenuItem {

		private final GamePlayer gamePlayer;
		private final Setting setting;

		public SetSettingItem(GamePlayer gamePlayer, Setting setting, String displayName, Material icon, String... lore) {
			super(displayName, new ItemStack(icon), lore);
			this.gamePlayer = gamePlayer;
			this.setting = setting;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			if (setting == Setting.MESSAGE_TYPE) return;
			gamePlayer.setSetting(setting, !asBoolean(gamePlayer.getSetting(setting)));
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 0.4f);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> new SettingsMenu(gamePlayer).open(event.getPlayer()), 4L);
		}

		private boolean asBoolean(Object object) {
			return object instanceof Boolean && (boolean) object;
		}
	}

	private static class MessageTypeItem extends MenuItem {

		private final GamePlayer gamePlayer;

		public MessageTypeItem(GamePlayer gamePlayer) {
			super(Langauge.MENU_MY_PROFILE_SETTINGS_MESSAGES_TYPE_ITEM_NAME.toString(),
					new ItemStack(Material.SLIME_BALL),
					ColourUtil.colouredLore(Langauge.MENU_MY_PROFILE_SETTINGS_MESSAGES_TYPE_ITEM_LORE.toString()
							.replace("%TYPE%", (String) gamePlayer.getSetting(Setting.MESSAGE_TYPE)))
							.toArray(String[]::new));
			this.gamePlayer = gamePlayer;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			String current = (String) gamePlayer.getSetting(Setting.MESSAGE_TYPE);
			if (current == null) current = "all";
			int idx = -1;
			for (int i = 0; i < MESSAGE_TYPES.length; i++) {
				if (MESSAGE_TYPES[i].equals(current)) { idx = i; break; }
			}
			String next = MESSAGE_TYPES[(idx + 1) % MESSAGE_TYPES.length];
			gamePlayer.setSetting(Setting.MESSAGE_TYPE, next);
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 0.4f);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> new SettingsMenu(gamePlayer).open(event.getPlayer()), 4L);
		}
	}

	private static class BackToMyProfileItem extends MenuItem {

		public BackToMyProfileItem() {
			super(Langauge.MENU_BACK_ITEM_NAME.toString(), new ItemStack(Material.BARRIER));
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			event.setWillClose(true);
			Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
				new MyProfileMenu(event.getPlayer()).open(event.getPlayer());
			}, 4L);
		}
	}

}

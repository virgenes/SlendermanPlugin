package me.dreamdevs.slender.api;

import lombok.Getter;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.configuration.file.YamlConfiguration;



public enum Langauge {

	EMPTY("",""),

	// Arena chat announcements
	ARENA_PLAYER_JOIN("ArenaAnnouncements.Player-Join", "&e%PLAYER% joined to this arena &7(&b%CURRENT%/%MAXIMUM%&7)"),
	ARENA_PLAYER_QUIT("ArenaAnnouncements.Player-Quit", "&e%PLAYER% left arena &7(&b%CURRENT%/%MAXIMUM%&7)"),

	ARENA_KILLED_BY_SLENDER_MAN("ArenaAnnouncements.Survivor-Killed-By-SlenderMan", "&e%PLAYER% was killed by SlenderMan!"),
	ARENA_KILLED_BY_SURVIVOR("ArenaAnnouncements.SlenderMan-Killed-By-Survivor", "&eSlenderMan was killed, be careful, he came back!"),

	ARENA_SLENDER_MAN_LEFT("ArenaAnnouncements.SlenderMan-Left", "&eSlenderMan left the game!"),
	ARENA_SURVIVOR_LEFT("ArenaAnnouncements.Survivor-Left", "&e%PLAYER% left the game!"),

	ARENA_COLLECTED_PAGES("ArenaAnnouncements.Collected-Pages","&6Collected pages: %CURRENT%"),

	ARENA_JOIN_GAME_INFO("ArenaAnnouncements.Join-Game-Info", "&a&l❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚\\n&r\\n                    &f&lSlendermanPlugin           \\n&r\\n\\n    &e&lFind all pages or die by SlenderMan!\n" +
			"                         \\n   &e&lUse your torches to light up a little!\\n&r\\n&a&l❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚"),

	ARENA_STARTED_INFO("ArenaAnnouncements.Started-Info","&eThe game has begun! Find or kill them all!"),
	ARENA_STOPPED_STARTING("ArenaAnnouncements.Stop-Starting", "&cWe need few more people to start the game..."),

	ARENA_STARTING_INFO("ArenaAnnouncements.Starting-Info", "&eStarting in 30 seconds..."),
	ARENA_PAGE_SPAWNED_INFO("ArenaAnnouncements.Page-Spawned-Info","&eThe page was spawned somewhere in the dark!"),
	ARENA_SPECTATOR_MODE("ArenaAnnouncements.Spectator-Mode","&eYou are now spectator!"),

	// Arena titles announcements
	ARENA_TITLE("ArenaAnnouncements.Arena-Title","&c&lSlendermanPlugin!"),
	ARENA_WAITING_SUBTITLE("ArenaAnnouncements.Waiting-Subtitle","&7Waiting for players..."),
	ARENA_STARTING_SUBTITLE("ArenaAnnouncements.Starting-Subtitle","&aStarting in %TIME% seconds..."),
	ARENA_STARTED_SUBTITLE("ArenaAnnouncements.Started-Subtitle","&4Good Luck!"),

	ARENA_WIN_SURVIVORS_SUBTITLE("ArenaAnnouncements.Win-Survivors-Subtitle","&aSurvivors won the game!"),
	ARENA_WIN_SLENDERMAN_SUBTITLE("ArenaAnnouncements.Win-SlenderMan-Subtitle","&cSlenderMan won the game!"),

	ARENA_GAME_OVER_TITLE("ArenaAnnouncements.Game-Over-Title","&c&lGAME OVER"),
	ARENA_GAME_OVER_SUBTITLE("ArenaAnnouncements.Game-Over-Subtitle","&7Thanks for playing!"),

	ARENA_DEAD_TITLE("ArenaAnnouncements.Dead-Title","&c&lYou died!"),
	ARENA_DEAD_SUBTITLE("ArenaAnnouncements.Dead-Subtitle","&eYou are now spectator!"),

	ARENA_BOSS_BAR_WAITING_TITLE("ArenaAnnouncements.Boss-Bar.Waiting-Title","&c&lSlendermanPlugin"),
	ARENA_BOSS_BAR_RUNNING_TITLE("ArenaAnnouncements.Boss-Bar-Running-Title","&cTime left: %TIME% seconds"),
	ARENA_BOSS_BAR_ENDING_TITLE("ArenaAnnouncements.Boss-Bar.Ending-Title","&cTeleport to lobby in %TIME% seconds"),

	ARENA_NO_SLOTS("ArenaAnnouncements.No-Slots","&cThis game is full!"),
	ARENA_NO_AVAILABLE_ARENAS("ArenaAnnouncements.No-Available-Arenas","&cThere is no available arena!"),
	ARENA_NO_PERMISSION("ArenaAnnouncements.No-Permission","&cYou do not have permission to do this!"),
	ARENA_NO_ARGUMENT("ArenaAnnouncements.No-Argument","&cArgument does not exist!"),
	ARENA_NO_ARENA("ArenaAnnouncements.No-Arena","&cThere is no map with this ID!"),
	ARENA_NO_PLAYER("ArenaAnnouncements.No-Player","&cThere is no player!"),
	ARENA_TOO_MANY_ARGUMENTS("ArenaAnnouncements.Too-Many-Arguments","&cToo many arguments!"),
	ARENA_STILL_RUNNING("ArenaAnnouncements.Arena-Still-Running","&cThis game is running!"),
	ARENA_PLAYER_IN_GAME("ArenaAnnouncements.Player-In-Game","&cYou are already in game!"),

	ARENA_STATUS_WAITING("ArenaAnnouncements.Status.Waiting","&7Waiting..."),
	ARENA_STATUS_STARTING("ArenaAnnouncements.Status.Starting","&aStarting"),
	ARENA_STATUS_RUNNING("ArenaAnnouncements.Status.Running","&bRunning"),
	ARENA_STATUS_ENDING("ArenaAnnouncements.Status.Ending","&cEnding"),
	ARENA_STATUS_RESTARTING("ArenaAnnouncements.Status.Restarting","&cRestarting"),

	// Game objectives like pages, teams, etc.
	ARENA_PAGE_NUMBER("ArenaAnnouncements.Objectives.Page-Name","&6Page %NUMBER%"),
	ARENA_SURVIVOR_TEAM("ArenaAnnouncements.Objectives.Survivor-Team","&bSurvivors"),
	ARENA_SLENDERMAN_TEAM("ArenaAnnouncements.Objectives.SlenderMan-Team","&cSlenderMan"),

	LOBBY_CLICK_HERE("LobbyAnnouncements.Click-Here", "&eClick here!"),
	LOBBY_ARENA_STARTING_ANNOUNCEMENT("LobbyAnnouncements.Arena-Starting-Announcement","&a&l❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚\\n&r\\n                    &f&lSlendermanPlugin           \\n&r\\n"+
			"&b&lArena &f&l%ARENA% &b&lis starting in 30 seconds!\\n&r\\n&a&l❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚"),

	LEVEL_PLAYER_EXP_REWARD("Level.Player-Exp-Reward","&a+%AMOUNT% Exp"),
	LEVEL_PLAYER_LEVEL_UP("Level.Player-Level-Up","&a&l❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚\\n&r\\n                    &f&lSlendermanPlugin           \\n&r\\n\\n                        &3&lLevel Up!"+
			"\n   &7You are getting better and better!\n     &7Thanks for playing this game!\n      &7Your current level: &b%LEVEL%\\n&r\\n&a&l❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚❚"),

	ITEMS_ARENA_SELECTOR_DISPLAY_NAME("Items.Arena-Selector.DisplayName","&aArena Selector &7(Right-click)"),
	ITEMS_ARENA_SELECTOR_DISPLAY_LORE("Items.Arena-Selector.DisplayLore","\n&eRight-click to open &aArena Selector&e."),
	ITEMS_LEAVE_DISPLAY_NAME("Items.Leave.DisplayName","&cLeave &7(Right-click)"),
	ITEMS_LEAVE_DISPLAY_LORE("Items.Leave.DisplayLore","\n&eRight click to leave arena."),
	ITEMS_MY_PROFILE_DISPLAY_NAME("Items.My-Profile.DisplayName","&bMy Profile &7(Right-click)"),
	ITEMS_MY_PROFILE_DISPLAY_LORE("Items.My-Profile.DisplayLore","\n&eRight-click to open &bMy Profile&e."),
	ITEMS_PLAY_AGAIN_DISPLAY_NAME("Items.Play-Again.DisplayName","&bPlay Again &7(Right-click)"),
	ITEMS_PLAY_AGAIN_DISPLAY_LORE("Items.Play-Again.DisplayLore","\n&eRight-click to &bPlay Again&e."),
	ITEMS_PARTY_MENU_DISPLAY_NAME("Items.Party-Menu.DisplayName","&bParty Menu &7(Right-click)"),
	ITEMS_PARTY_MENU_DISPLAY_LORE("Items.Party-Menu.DisplayLore","\n&eRight-click to open &bParty Menu&e."),
	ITEMS_PERKS_DISPLAY_NAME("Items.Perks.DisplayName","&dPerks &7(Right-click)"),
	ITEMS_PERKS_DISPLAY_LORE("Items.Perks.DisplayLore","\n&eRight-click to open &dPerks&e."),
	ITEMS_SPECTATOR_SETTINGS_DISPLAY_NAME("Items.Spectator-Settings.DisplayName","&bSpectator Settings &7(Right-click)"),
	ITEMS_SPECTATOR_SETTINGS_DISPLAY_LORE("Items.Spectator-Settings.DisplayLore","\n&eRight-click to open &bSpectator Settings&e."),
	ITEMS_SPECTATOR_TELEPORT_DISPLAY_NAME("Items.Spectator-Teleport.DisplayName","&aTeleporter &7(Right-click)"),
	ITEMS_SPECTATOR_TELEPORT_DISPLAY_LORE("Items.Spectator-Teleport-DisplayLore","\n&eRight-click to open &aTeleporter&e."),
	ITEMS_SLENDERMAN_SWORD_DISPLAY_NAME("Items.SlenderMan-Sword.DisplayName","&cSlenderMan's Sword"),
	ITEMS_SLENDERMAN_SWORD_DISPLAY_LORE("Items.SlenderMan-Sword.DisplayLore","\n&4Kill them all!"),
	ITEMS_SLENDERMAN_COMPASS_DISPLAY_NAME("Items.SlenderMan-Compass.DisplayName","&cSlenderMan's Compass"),
	ITEMS_SLENDERMAN_COMPASS_DISPLAY_LORE("Items.SlenderMan-Compass.DisplayLore","\n&4Find them all!"),
	ITEMS_SURVIVOR_SWORD_DISPLAY_NAME("Items.Survivor-Sword.DisplayName","&cSurvivor's Sword"),
	ITEMS_SURVIVOR_SWORD_DISPLAY_LORE("Items.Survivor-Sword.DisplayLore","\n&7Use this sword to protect\n&7friends and yourself!"),

	ADMIN_ONLY_PLAYER("Admin.Only-Player-Command","&cConsole cannot performs this command."),
	ADMIN_SET_LOBBY_SUCCESSFULLY("Admin.Set-Lobby-Successfully","&aSuccess! You set the lobby!"),
	ADMIN_SET_SLENDERMAN_SPAWN_SUCCESSFULLY("Admin.Set-SlenderMan-Spawn-Successfully","&aYou set SlenderMan spawn location!"),
	ADMIN_SET_SURVIVORS_SPAWN_SUCCESSFULLY("Admin.Set-Survivors-Spawn-Successfully","&aYou added Survivors spawn location!"),
	ADMIN_SET_PAGES_SPAWN_SUCCESSFULLY("Admin.Set-Pages-Spawn-Successfully","&aYou added pages spawn location!"),
	ADMIN_SAVED_ARENA_SETTINGS_SUCCESSFULLY("Admin.Saved-Arena-Settings-Successfully","&aYou saved arena settings successfully!"),
	ADMIN_FORCE_START_ARENA_SUCCESSFULLY("Admin.Force-Start-Arena-Successfully","&aSuccessfully forced to start the game!"),
	ADMIN_FORCE_START_ARENA_UNSUCCESSFULLY("Admin.Force-Start-Arena-Unsuccessfully","&cCouldn't force to start the game!"),
	ADMIN_FORCE_STOP_ARENA_SUCCESSFULLY("Admin.Force-Stop-Arena-Successfully","&aSuccessfully forced to stop the game!"),
	ADMIN_FORCE_STOP_ARENA_UNSUCCESSFULLY("Admin.Force-Stop-Arena-Unsuccessfully","&cCouldn't force to stop the game!"),
	ADMIN_FORCE_RESTART_ARENA_SUCCESSFULLY("Admin.Force-Restart-Arena-Successfully","&aSuccessfully forced to restart the game!"),

	PARTY_CREATED_INFO("Party.Created-Info","&aYou created party!"),
	PARTY_REMOVED_INFO("Party.Removed-Info","&cThe party was removed!"),
	PARTY_PLAYER_NOT_IN_PARTY("Party.Player-Not-In-Party","&cYou are not in party!"),
	PARTY_PLAYER_NOT_LEADER("Party.Player-Not-Leader","&cYou are not party leader!"),
	PARTY_NO_PLAYER("Party.No-Player","&cThis player is not in your party!"),
	PARTY_KICKED_PLAYER("Party.Kicked-Player","&b%PLAYER% was kicked from party by %LEADER%"),
	PARTY_NO_PUBLIC_PARTY("Party.No-Public-Party","&cThere is no public party!"),
	PARTY_IS_ALREADY_IN_PARTY("Party.Player-Is-Already-In-Party","&cYou are already in party!"),
	PARTY_PLAYER_JOINED_PARTY("Party.Player-Joined","&e%PLAYER% joined to party!"),
	PARTY_PLAYER_LEFT_PARTY("Party.Player-Left","&e%PLAYER% left from party!"),
	PARTY_CHANGED_STATUS("Party.Changed-Status","&b%LEADER% changed party status to: %STATUS%"),
	PLAYER_NOT_PENDING_REQUEST("Party.Not-Pending-Request","&cYou do not have pending request to join to the party!"),
	PARTY_INVITED_PLAYER("Party.Invited-Player","&aYou invited %TARGET% to your party!"),
	PARTY_REQUEST_MESSAGE("Party.Request-Message","&7You have new request to join to the party (click here to accept)"),
	PARTY_TOO_MANY_PLAYERS("Party.Too-Many-Players","&cYou cannot join to this arena, because your party has too many members!"),

	PERKS_SELECTED("Perks.Perk-Selected","&aYou selected %PERK_NAME% &afor %TEAM%"),

	MENU_STATUS_ON("Menu.Status-On","&aOn"),
	MENU_STATUS_OFF("Menu.Status-Off","&cOff"),
	MENU_BACK_ITEM_NAME("Menu.Back-Item-Name","&cBack"),

	MENU_ARENA_SELECTOR_TITLE("Menu.Arena-Selector.Title","&8» Select Arena"),
	MENU_ARENA_SELECT_ARENA_ITEM_NAME("Menu.Arena-Selector.Item-Name","&aArena: &b%MAP_NAME%"),
	MENU_ARENA_SELECT_ARENA_ITEM_LORE("Menu.Arena-Selector.Item-Lore","&r\n&7» Players: &b%CURRENT_PLAYERS%&7/&a%MAX_PLAYERS%\n&7» Arena Status: %ARENA_STATUS%\n&r\n&eClick to join to this arena."),

	MENU_MY_PROFILE_TITLE("Menu.My-Profile.Title","My Profile"),
	MENU_MY_PROFILE_SETTINGS_TITLE("Menu.My-Profile.Settings.Title","Settings"),
	MENU_MY_PROFILE_STATS_ITEM_NAME("Menu.My-Profile.Stats-Item-Name","&bYour Stats"),
	MENU_MY_PROFILE_STATS_ITEM_LORE("Menu.My-Profile.Stats-Item-Lore","&7Wins: &b%WINS%\n&r\n&7Level: &b%LEVEL%\n&7Exp: &b%EXP%\n&r\n&7Collected Pages: &b%COLLECTED_PAGES%\n&r\n&7Killed Survivors: &b%KILLED_SURVIVORS%\n&7Killed SlenderMen: &b%KILLED_SLENDERMEN%\n&7Total Kills: &b%TOTAL_KILLS%"),
	MENU_MY_PROFILE_SETTINGS_ITEM_NAME("Menu.My-Profile.Settings-Item-Name","&bSettings"),
	MENU_MY_PROFILE_SETTINGS_ITEM_LORE("Menu.My-Profile.Settings-Item-Lore","\n&7Click here to change your settings!"),
	MENU_MY_PROFILE_SETTINGS_AUTO_JOIN_MODE_ITEM_NAME("Menu.My-Profile.Settings.Auto-Join-Mode-Item-Name","&bAuto Join Mode"),
	MENU_MY_PROFILE_SETTINGS_AUTO_JOIN_MODE_ITEM_LORE("Menu.My-Profile.Settings.Auto-Join-Mode-Item-Lore","\n&7If this option is on,\n&7you will be teleported to another\n&7arena, after your game ends.\n&7\n&7Status: %STATUS%"),
	MENU_MY_PROFILE_SETTINGS_SH0W_ARENA_JOIN_MESSAGE_ITEM_NAME("Menu.My-Profile.Settings.Show-Arena-Join-Message-Item-Name","&bShow Arena Join Message"),
	MENU_MY_PROFILE_SETTINGS_SHOW_ARENA_JOIN_MESSAGE_ITEM_LORE("Menu.My-Profile.Settings.Show-Arena-Join-Message-Item-Lore","\n&7If this option is on,\n&7you will get information about SlendermanPlugin game\n&7\n&7Status: %STATUS%"),
	MENU_MY_PROFILE_SETTINGS_MESSAGES_TYPE_ITEM_NAME("Menu.My-Profile.Settings.Messages-Type-Item-Name","&bMessages Type"),
	MENU_MY_PROFILE_SETTINGS_MESSAGES_TYPE_ITEM_LORE("Menu.My-Profile.Settings.Messages-Type-Item-Lore","\n&7This option enables you to\n&7choose, which messages you will be getting.\n&7\n&7Type: %TYPE%"),

	MENU_PERKS_TITLE("Menu.Perks.Title","Choose Perk"),
	MENU_PERKS_OPEN_SURVIVOR_PERKS("Menu.Perks.Survivor-Perks","&aSelect Survivor Perk"),
	MENU_PERKS_OPEN_SLENDERMAN_PERKS("Menu.Perks.SlenderMan-Perks","&aSelect SlenderMan Perk"),

	MENU_PARTY_TITLE("Menu.Party.Title","&3&lParty"),
	MENU_PARTY_PUBLIC("Menu.Party.Public","&aPublic"),
	MENU_PARTY_PRIVATE("Menu.Party.Private","&cPrivate"),
	MENU_PARTY_CREATE_ITEM_NAME("Menu.Party.Create-Item-Name","&bCreate Party"),
	MENU_PARTY_CREATE_ITEM_LORE("Menu.Party.Create-Item-Lore","\n&7Click to create party!"),
	MENU_PARTY_DELETE_ITEM_NAME("Menu.Party.Delete-Item-Name","&bDelete Party"),
	MENU_PARTY_DELETE_ITEM_LORE("Menu.Party.Delete-Item-Lore","\n&7Click to delete party!"),
	MENU_PARTY_CHANGE_STATUS_ITEM_NAME("Menu.Party.Change-Status-Item-Name","&aChange Status"),
	MENU_PARTY_CHANGE_STATUS_ITEM_LORE("Menu.Party.Change-Status-Item-Lore","\n&7Click to open or close the party!"),
	MENU_PARTY_FIND_PARTY_ITEM_NAME("Menu.Party.Find-Party-Item-Name","&aFind Party"),
	MENU_PARTY_FIND_PARTY_ITEM_LORE("Menu.Party.Find-Party-Item-Lore","\n&7Click to open public party list!"),
	MENU_PARTY_INFO_ITEM_NAME("Menu.Party.Info-Item-Name","&bParty Info"),
	MENU_PARTY_INFO_ITEM_LORE("Menu.Party.Info-Item-Lore","&7Leader: &b%LEADER%\n&7Members Count: %MEMBERS_COUNT%\n&7Status: %STATUS%"),

	MENU_SPECTATOR_TELEPORTER_TITLE("Menu.Spectator-Teleporter.Title","Teleporter Menu"),
	MENU_SPECTATOR_SETTINGS_TITLE("Menu.Spectator-Settings.Title","Spectator Settings"),
	MENU_SPECTATOR_SETTINGS_NO_SPEED_ITEM_NAME("Menu.Spectator-Settings.No-Speed","&cNo Speed"),
	MENU_SPECTATOR_SETTINGS_SPEED_ITEM_NAME("Menu.Spectator-Settings.Speed","&aSpeed"),

	MENU_LEVELS_TITLE("Menu.Levels.Title","Levels"),
	MENU_LEVELS_LEVEL_ITEM_NAME("Menu.Levels.Level-Item-Name","&eLevel %NUMBER%"),

	MENU_EDITOR_TITLE("Menu.Editor.Title","Map Editor"),
	MENU_EDITOR_MINIMUM_PLAYERS_ITEM_NAME("Menu.Editor.Minimum-Players-Item-Name","&bMinimum Players: %AMOUNT%"),
	MENU_EDITOR_MINIMUM_PLAYERS_ITEM_LORE("Menu.Editor.Minimum-Players-Item-Lore","\n&7Click izquierdo para subir la cantidad\n&7Click derecho para bajarla"),
	MENU_EDITOR_MAXIMUM_PLAYERS_ITEM_NAME("Menu.Editor.Maximum-Players-Item-Name","&bMaximum Players: %AMOUNT%"),
	MENU_EDITOR_MAXIMUM_PLAYERS_ITEM_LORE("Menu.Editor.Maximum-Players-Item-Lore","\n&7Click izquierdo para subir la cantidad\n&7Click derecho para bajarla"),
	MENU_EDITOR_SET_SLENDERMAN_SPAWN_ITEM_NAME("Menu.Editor.Set-SlenderMan-Spawn-Item-Name","&bSet SlenderMan Spawn"),
	MENU_EDITOR_SET_SLENDERMAN_SPAWN_ITEM_LORE("Menu.Editor.Set-SlenderMan-Spawn-Item-Lore","\n&7Click to set SlenderMan Spawn location"),
	MENU_EDITOR_GAME_TIME_ITEM_NAME("Menu.Editor.Game-Time-Item-Name","&bGame Time: %AMOUNT%"),
	MENU_EDITOR_GAME_TIME_ITEM_LORE("Menu.Editor.Game-Time-Item-Lore","\n&7Click izquierdo para subir la cantidad\n&7Click derecho para bajarla"),
	MENU_EDITOR_ADD_SURVIVOR_SPAWN_ITEM_NAME("Menu.Editor.Add-Survivor-Spawn-Item-Name","&bAdd Survivors Spawn"),
	MENU_EDITOR_ADD_SURVIVOR_SPAWN_ITEM_LORE("Menu.Editor.Add-Survivor-Spawn-Item-Lore","\n&7Click to add Survivors Spawn location"),
	MENU_EDITOR_ADD_PAGES_SPAWN_ITEM_NAME("Menu.Editor.Add-Pages-Spawn-Item-Name","&bAdd Pages Spawn"),
	MENU_EDITOR_ADD_PAGES_SPAWN_ITEM_LORE("Menu.Editor.Add-Pages-Spawn-Item-Lore","\n&7Click to add pages spawn location"),
	MENU_EDITOR_SAVE_SETTINGS_ITEM_NAME("Menu.Editor.Save-Settings-Item-Name","&bSave Settings"),
	MENU_EDITOR_SAVE_SETTINGS_ITEM_LORE("Menu.Editor.Save-Settings-Item-Lore","\n&7Click to save settings"),

	MENU_ADMIN_MENU_TITLE("Menu.Admin.Title","Admin Menu"),
	MENU_ADMIN_MENU_FORCE_START_GAME_ITEM("Menu.Admin.Force-Start-Game-Item","&aForce Start"),
	MENU_ADMIN_MENU_FORCE_RESTART_GAME_ITEM("Menu.Admin.Force-Restart-Game-Item","&aForce Restart"),
	MENU_ADMIN_MENU_FORCE_STOP_GAME_ITEM("Menu.Admin.Force-Stop-Game-Item","&aForce Stop"),
	COMPASS_NO_SURVIVORS("ArenaAnnouncements.Compass-No-Survivors","&cNo survivors available to track."),

	MENU_ADMIN_MENU_FORCE_SET_LOBBY_ITEM("Menu.Admin.Force-Set-Lobby-Game-Item","&aSet Lobby"),

	// Slender Shop
	SHOP_TITLE("Shop.Title","&4&lSlender Shop"),
	SHOP_SKIN_LOCKED("Shop.Skin-Locked","&cLocked &7- &6%COST% coins"),
	SHOP_SKIN_OWNED("Shop.Skin-Owned","&aOwned"),
	SHOP_SKIN_EQUIPPED("Shop.Skin-Equipped","&b&lEquipped"),
	SHOP_NOT_ENOUGH_COINS("Shop.Not-Enough-Coins","&cNot enough coins! You need &6%COST% &ccoins."),
	SHOP_SKIN_PURCHASED("Shop.Skin-Purchased","&aSkin &e%SKIN% &apurchased!"),
	SHOP_SKIN_EQUIPPED_MSG("Shop.Skin-Equipped-Msg","&aSkin &e%SKIN% &aequipped!"),
	ITEMS_SLENDER_SHOP_DISPLAY_NAME("Items.Slender-Shop.DisplayName","&4Slender Shop &7(Right-click)"),
	ITEMS_SLENDER_SHOP_DISPLAY_LORE("Items.Slender-Shop.DisplayLore","\n&eRight-click to open &4Slender Shop&e."),
	ITEMS_SHOP_DISPLAY_NAME("Items.Shop.DisplayName","&6Shop &7(Right-click)"),
	ITEMS_SHOP_DISPLAY_LORE("Items.Shop.DisplayLore","\n&eRight-click to open the &6Shop&e."),
	SHOP_SURVIVOR_SECTION("Shop.Survivor-Section","&aSurvivor Perks"),
	SHOP_SLENDER_SECTION("Shop.Slender-Section","&4Slender Skins"),

	// Torch messages
	TORCH_NO_USES("Torch.No-Uses","&cYou have no torches left!"),
	TORCH_COOLDOWN("Torch.Cooldown","&cTorch on cooldown! &7(%TIME%s)"),
	TORCH_USED("Torch.Used","&eTorch used. &7Remaining: &6%CURRENT%/%MAX%"),

	// Page pickup title
	ARENA_PAGE_PICKUP_TITLE("ArenaAnnouncements.Page-Pickup-Title","&6Page %CURRENT%/8");

	// Torch messages

	private static YamlConfiguration configuration;
	private final @Getter String defaultMessage;
	private final @Getter String path;

	Langauge(String path, String defaultMessage) {
		this.path = path;
		this.defaultMessage = defaultMessage;
	}

	public static void setConfiguration(YamlConfiguration configuration) {
		Langauge.configuration = configuration;
	}

	@Override
	public String toString() {
		if (configuration == null) return ColourUtil.colorize(defaultMessage);
		Object value = configuration.get(path);
		if (value instanceof String) {
			return ColourUtil.colorize((String) value);
		}
		return ColourUtil.colorize(defaultMessage);
	}

}
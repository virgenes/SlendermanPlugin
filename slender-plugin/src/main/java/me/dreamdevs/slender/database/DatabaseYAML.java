package me.dreamdevs.slender.database;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.database.IDatabase;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseYAML implements IDatabase {

	private File dataDirectory;

	@Override
	public void connect() {
		dataDirectory = new File(SlenderMain.getInstance().getDataFolder(), "users");
		if (!dataDirectory.exists() || !dataDirectory.isDirectory()) dataDirectory.mkdirs();
	}

	@Override
	public void disconnect() {
		// Nothing to do
	}

	@Override
	public void saveStatistics() {
		SlenderMain.getInstance().getPlayerManager().getPlayers().forEach(gamePlayer -> {
			if (gamePlayer.getPlayer() == null) return;
			File playerFile = new File(dataDirectory, gamePlayer.getPlayer().getUniqueId()+".yml");
			Util.createFile(playerFile);
			YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
			playerData.set("PlayerInfo.UUID", gamePlayer.getPlayer().getUniqueId().toString());
			playerData.set("PlayerInfo.Nick", gamePlayer.getPlayer().getName());

			playerData.set("Statistics.Coins", gamePlayer.getStatistic(Statistic.COINS));
			playerData.set("Statistics.Level", gamePlayer.getStatistic(Statistic.LEVEL));
			playerData.set("Statistics.Exp", gamePlayer.getStatistic(Statistic.EXP));

			playerData.set("Statistics.Wins", gamePlayer.getStatistic(Statistic.WINS));
			playerData.set("Statistics.CollectedPages", gamePlayer.getStatistic(Statistic.COLLECTED_PAGES));
			playerData.set("Statistics.KilledSurvivors", gamePlayer.getStatistic(Statistic.KILLED_SURVIVORS));
			playerData.set("Statistics.KilledSlenderMen", gamePlayer.getStatistic(Statistic.KILLED_SLENDERMEN));
			playerData.set("PlayerSettings.AutoArenaJoin", gamePlayer.getSetting(Setting.AUTO_JOIN_MODE));
			playerData.set("PlayerSettings.ShowJoinArenaMessage", gamePlayer.getSetting(Setting.SHOW_ARENA_JOIN_MESSAGE));
			playerData.set("PlayerSettings.MessagesType", gamePlayer.getSetting(Setting.MESSAGE_TYPE));
			Perk survivorPerk = gamePlayer.getPerk(Role.SURVIVOR);
			Perk slenderPerk = gamePlayer.getPerk(Role.SLENDER);
			playerData.set("PlayerPerks.EquippedSurvivorPerk", survivorPerk != null ? survivorPerk.getClass().getAnnotation(PerkInfo.class).name() : "RUNAWAY");
			playerData.set("PlayerPerks.EquippedSlenderManPerk", slenderPerk != null ? slenderPerk.getClass().getAnnotation(PerkInfo.class).name() : "ENDERMAN");
			playerData.set("PlayerSkins.Owned", gamePlayer.getOwnedSkins().stream().map(SlenderDisguise::name).collect(Collectors.toList()));
			playerData.set("PlayerSkins.Equipped", gamePlayer.getEquippedSkin().name());
			try {
				playerData.save(playerFile);
			} catch (Exception e) {}
		});
	}

	@Override
	public void loadStatistics() {
		File[] files = dataDirectory.listFiles(((dir, name) -> name.endsWith(".yml")));

		if (files == null || files.length == 0) {
			return;
		}

		Stream.of(files).map(YamlConfiguration::loadConfiguration).forEach(configuration -> {
			UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString("PlayerInfo.UUID")));

			GamePlayer gamePlayer = new GamePlayer(Bukkit.getOfflinePlayer(uuid));
			gamePlayer.setStatistic(Statistic.COINS, configuration.getInt("Statistics.Coins",0));
			gamePlayer.setStatistic(Statistic.LEVEL, configuration.getInt("Statistics.Level",0));
			gamePlayer.setStatistic(Statistic.EXP, configuration.getInt("Statistics.Exp",0));
			gamePlayer.setStatistic(Statistic.COLLECTED_PAGES, configuration.getInt("Statistics.CollectedPages",0));
			gamePlayer.setStatistic(Statistic.WINS, configuration.getInt("Statistics.Wins",0));
			gamePlayer.setStatistic(Statistic.KILLED_SURVIVORS, configuration.getInt("Statistics.KilledSurvivors",0));
			gamePlayer.setStatistic(Statistic.KILLED_SLENDERMEN, configuration.getInt("Statistics.KilledSlenderMen",0));

			gamePlayer.setSetting(Setting.AUTO_JOIN_MODE, configuration.getBoolean("PlayerSettings.AutoArenaJoin",false));
			gamePlayer.setSetting(Setting.SHOW_ARENA_JOIN_MESSAGE, configuration.getBoolean("PlayerSettings.ShowJoinArenaMessage",true));
			gamePlayer.setSetting(Setting.MESSAGE_TYPE, configuration.getString("PlayerSettings.MessagesType","all"));

			gamePlayer.setPerk(Role.SURVIVOR, SlenderMain.getInstance().getPerkManager().getPerk(configuration.getString("PlayerPerks.EquippedSurvivorPerk","RUNAWAY")));
			gamePlayer.setPerk(Role.SLENDER, SlenderMain.getInstance().getPerkManager().getPerk(configuration.getString("PlayerPerks.EquippedSlenderManPerk","")));

			List<String> ownedSkins = configuration.getStringList("PlayerSkins.Owned");
			if (ownedSkins.isEmpty()) {
				gamePlayer.getOwnedSkins().add(SlenderDisguise.ENDERMAN);
			} else {
				for (String skinName : ownedSkins) {
					try {
						SlenderDisguise skin = SlenderDisguise.valueOf(skinName.toUpperCase());
						gamePlayer.getOwnedSkins().add(skin);
					} catch (IllegalArgumentException ignored) {}
				}
			}
			String equippedName = configuration.getString("PlayerSkins.Equipped", "ENDERMAN");
			try {
				SlenderDisguise equipped = SlenderDisguise.valueOf(equippedName.toUpperCase());
				if (gamePlayer.ownsSkin(equipped)) {
					gamePlayer.equipSkin(equipped);
				}
			} catch (IllegalArgumentException ignored) {}

			SlenderMain.getInstance().getPlayerManager().getPlayers().add(gamePlayer);
		});
	}
}
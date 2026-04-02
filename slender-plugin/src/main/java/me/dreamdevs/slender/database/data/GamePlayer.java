package me.dreamdevs.slender.database.data;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.database.IGamePlayer;
import me.dreamdevs.slender.api.game.IArena;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GamePlayer implements IGamePlayer {

	private final OfflinePlayer player;
	private final Map<Statistic, Integer> statistics;
	private final Map<Setting, Object> settings;
	private final Map<Role, Perk> perks;
	private final Set<String> ownedPerks;
	private final Set<SlenderDisguise> ownedSkins;
	private final Map<me.dreamdevs.slender.api.game.Skill, Integer> skills;
	private SlenderDisguise equippedSkin;

	public GamePlayer(OfflinePlayer player) {
		this.player = player;
		this.statistics = new HashMap<>();
		this.settings = new HashMap<>();
		this.perks = new HashMap<>();
		this.ownedPerks = new HashSet<>();
		this.ownedSkins = new HashSet<>();
		this.skills = new HashMap<>();
		this.equippedSkin = SlenderDisguise.ENDERMAN;
		ownedSkins.add(SlenderDisguise.ENDERMAN);
	}

	@Override
	public int getSkillLevel(me.dreamdevs.slender.api.game.Skill skill) {
		return skills.getOrDefault(skill, 0);
	}

	@Override
	public void setSkillLevel(me.dreamdevs.slender.api.game.Skill skill, int level) {
		this.skills.put(skill, level);
	}

	@Override
	public void unlockPerk(String perkName) {
		this.ownedPerks.add(perkName);
	}

	@Override
	public boolean ownsPerk(String perkName) {
		return ownedPerks.contains(perkName);
	}

	@Override
	public Set<String> getOwnedPerks() {
		return ownedPerks;
	}

	@Override
	public OfflinePlayer getOfflinePlayer() {
		return player;
	}

	@Override
	public void setStatistic(Statistic statistic, int value) {
		this.statistics.put(statistic, value);
	}

	@Override
	public int getStatistic(Statistic statistic) {
		return statistics.getOrDefault(statistic, 0);
	}

	@Override
	public Object getSetting(Setting setting) {
		return settings.get(setting);
	}

	@Override
	public void setSetting(Setting setting, Object value) {
		this.settings.put(setting, value);
	}

	@Override
	public void setPerk(Role role, Perk perk) {
		this.perks.put(role, perk);
	}

	@Override
	public Perk getPerk(Role role) {
		return perks.get(role);
	}

	@Override
	public IArena getArena() {
		Player onlinePlayer = getPlayer();
		if (onlinePlayer == null) return null;
		return SlenderMain.getInstance().getGameManager().getArenas().stream()
				.filter(arena -> arena.getPlayers().containsKey(onlinePlayer))
				.findFirst()
				.orElse(null);
	}

	@Override
	public boolean isInArena() {
		return getArena() != null;
	}

	public Player getPlayer() {
		return player.getPlayer();
	}

	public void clearInventory() {
		getPlayer().getInventory().clear();
		getPlayer().getInventory().setArmorContents(null);
		getPlayer().getInventory().setExtraContents(null);
	}

	public Set<SlenderDisguise> getOwnedSkins() {
		return ownedSkins;
	}

	public boolean ownsSkin(SlenderDisguise skin) {
		return ownedSkins.contains(skin);
	}

	public void purchaseSkin(SlenderDisguise skin) {
		ownedSkins.add(skin);
	}

	public SlenderDisguise getEquippedSkin() {
		return equippedSkin != null ? equippedSkin : SlenderDisguise.ENDERMAN;
	}

	public void equipSkin(SlenderDisguise skin) {
		if (ownedSkins.contains(skin)) {
			this.equippedSkin = skin;
		}
	}
}
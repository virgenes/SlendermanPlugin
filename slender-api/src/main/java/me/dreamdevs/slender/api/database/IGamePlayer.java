package me.dreamdevs.slender.api.database;

import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.game.IArena;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import org.bukkit.OfflinePlayer;


public interface IGamePlayer {

	OfflinePlayer getOfflinePlayer();

	/**
	 * This allows you to set or get any statistic, even if there will be new statistic.
	 */

	void setStatistic(Statistic statistic, int value);

	int getStatistic(Statistic statistic);

	/**
	 * Same way in settings, like in statistics, you can do anything you want.
	 */

	Object getSetting(Setting setting);

	void setSetting(Setting setting, Object value);

	/**
	 * Perk methods
	 */

	void setPerk(Role role, Perk perk);

	Perk getPerk(Role role);

	void unlockPerk(String perkName);

	boolean ownsPerk(String perkName);

	java.util.Set<String> getOwnedPerks();

	int getSkillLevel(me.dreamdevs.slender.api.game.Skill skill);

	void setSkillLevel(me.dreamdevs.slender.api.game.Skill skill, int level);

	/**
	 * Arena methods
	 */

	IArena getArena();

	boolean isInArena();

}
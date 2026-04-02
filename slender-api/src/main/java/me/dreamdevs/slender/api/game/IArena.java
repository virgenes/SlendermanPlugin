package me.dreamdevs.slender.api.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

public interface IArena {

	String getId();

	int getMinPlayers();

	void setMinPlayers(int minPlayers);

	int getMaxPlayers();

	void setMaxPlayers(int maxPlayers);

	void setArenaState(ArenaState arenaState);

	ArenaState getArenaState();

	int getTimer();

	void setTimer(int timer);

	Map<Player, Role> getPlayers();

	Location getSlenderManSpawnLocation();

	int getGameTime();

	void setGameTime(int gameTime);

	void endGame(Role winner);

}
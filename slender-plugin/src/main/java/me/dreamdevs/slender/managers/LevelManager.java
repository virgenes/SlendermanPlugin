package me.dreamdevs.slender.managers;

import lombok.Getter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.SlenderPlayerExpGainEvent;
import me.dreamdevs.slender.api.events.SlenderPlayerLevelUpEvent;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

@Getter
public class LevelManager {

    private final Map<Integer, Level> levels;
    private YamlConfiguration config;

    public LevelManager() {
        this.levels = new HashMap<>();
        load(SlenderMain.getInstance());
    }

    public final void load(SlenderMain plugin) {
        this.levels.clear();
        config = YamlConfiguration.loadConfiguration(plugin.getLevelsFile());

        ConfigurationSection section = config.getConfigurationSection("Levels");
        if (section == null) return;

        for(String string : section.getKeys(false)) {
            int reqExp = section.getInt(string + ".Require-Exp", section.getInt(string + ".RequireExp", 0));
            java.util.List<String> rewards = section.getStringList(string + ".Rewards");
            Level level = new Level(reqExp, rewards);
            levels.put(Integer.parseInt(string), level);
        }

        Util.sendPluginMessage("&aRegistered " + levels.size() + " levels!");
    }

    public void addExp(GamePlayer gamePlayer, int exp) {
        int currentExp = gamePlayer.getStatistic(Statistic.EXP) + exp;
        gamePlayer.setStatistic(Statistic.EXP, currentExp);
        
        if (gamePlayer.getPlayer() != null) {
            gamePlayer.getPlayer().sendMessage(me.dreamdevs.slender.api.utils.Util.color(Langauge.LEVEL_PLAYER_EXP_REWARD.toString().replace("%AMOUNT%", String.valueOf(exp))));
        }

        SlenderPlayerExpGainEvent expEvent = new SlenderPlayerExpGainEvent(gamePlayer, exp);
        Bukkit.getServer().getPluginManager().callEvent(expEvent);

        checkLevelUp(gamePlayer);
    }

    public void checkLevelUp(GamePlayer gamePlayer) {
        int currentLevel = gamePlayer.getStatistic(Statistic.LEVEL);
        int nextLevel = currentLevel + 1;

        if (!levels.containsKey(nextLevel)) return;

        Level levelData = levels.get(nextLevel);
        int currentExp = gamePlayer.getStatistic(Statistic.EXP);

        if (currentExp >= levelData.getRequireExp()) {
            // Level Up!
            gamePlayer.setStatistic(Statistic.LEVEL, nextLevel);
            
            if (gamePlayer.getPlayer() != null) {
                gamePlayer.getPlayer().sendMessage(me.dreamdevs.slender.api.utils.Util.color(Langauge.LEVEL_PLAYER_LEVEL_UP.toString().replace("%LEVEL%", String.valueOf(nextLevel))));
                gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }

            SlenderPlayerLevelUpEvent levelEvent = new SlenderPlayerLevelUpEvent(gamePlayer, nextLevel);
            Bukkit.getServer().getPluginManager().callEvent(levelEvent);

            rewardPlayer(gamePlayer, levelData);

            // Check if they can level up again
            checkLevelUp(gamePlayer);
        }
    }

    private void rewardPlayer(GamePlayer gamePlayer, Level level) {
        if (level.getRewards() == null) return;

        for (String reward : level.getRewards()) {
            String[] split = reward.split(":");
            if (split.length < 2) continue;

            String type = split[0].toLowerCase();
            String value = split[1];

            switch (type) {
                case "coins":
                    int coins = Integer.parseInt(value);
                    gamePlayer.setStatistic(Statistic.COINS, gamePlayer.getStatistic(Statistic.COINS) + coins);
                    if (gamePlayer.getPlayer() != null) {
                        gamePlayer.getPlayer().sendMessage(me.dreamdevs.slender.api.utils.Util.color("&6&l+ " + coins + " Coins &7(Level Reward)"));
                    }
                    break;
                case "perk":
                    gamePlayer.unlockPerk(value);
                    if (gamePlayer.getPlayer() != null) {
                        gamePlayer.getPlayer().sendMessage(me.dreamdevs.slender.api.utils.Util.color("&b&lUNLOCKED PERK: &f" + value));
                    }
                    break;
                case "command":
                    String cmd = reward.substring(reward.indexOf(":") + 1).replace("%player%", gamePlayer.getPlayer().getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    break;
            }
        }
    }

}
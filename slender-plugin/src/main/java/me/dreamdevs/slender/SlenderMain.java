package me.dreamdevs.slender;

import lombok.Getter;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.SlenderApi;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.commands.CommandHandler;
import me.dreamdevs.slender.commands.PartyCommandHandler;
import me.dreamdevs.slender.commands.economy.EconomyCommand;
import me.dreamdevs.slender.database.Database;
import me.dreamdevs.slender.disguise.DisguiseListener;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.game.Lobby;
import me.dreamdevs.slender.listeners.GameListeners;
import me.dreamdevs.slender.listeners.PerksListeners;
import me.dreamdevs.slender.listeners.PlayerInteractListener;
import me.dreamdevs.slender.listeners.PlayerListeners;
import me.dreamdevs.slender.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Getter
public class SlenderMain extends JavaPlugin {

    private @Getter static SlenderMain instance;
    private PlayerManager playerManager;
    private LevelManager levelManager;
    private PartyManager partyManager;
    private GameManager gameManager;
    private PerkManager perkManager;

    private Database database;

    private Lobby lobby;

    // Files
    private File levelsFile;

    @Override
    public void onEnable() {
        instance = this;

        SlenderApi.loadApi(this);

        this.levelsFile = new File(getDataFolder(), "levels.yml");
        if (!levelsFile.exists()) {
            saveResource("levels.yml", true);
        }

        loadConfig();
        loadLang();

        saveDefaultArena();

        this.playerManager = new PlayerManager();

        this.perkManager = new PerkManager();

        this.database = new Database();
        this.database.connect(Config.DATABASE_TYPE.toString());
        this.database.loadData();

        this.gameManager = new GameManager();
        this.gameManager.loadGames();
        this.lobby = new Lobby();
        this.partyManager = new PartyManager();

        this.levelManager = new LevelManager();

        EconomyCommand economyCommand = new EconomyCommand();
        CommandHandler commandHandler = new CommandHandler(economyCommand);
        commandHandler.register(this);
        PartyCommandHandler partyCommandHandler = new PartyCommandHandler();
        partyCommandHandler.register(this);

        getServer().getPluginManager().registerEvents(new PlayerListeners(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new GameListeners(), this);
        getServer().getPluginManager().registerEvents(new PerksListeners(), this);
        getServer().getPluginManager().registerEvents(new DisguiseListener(), this);
        getServer().getPluginManager().registerEvents(new me.dreamdevs.slender.listeners.PlayerSkillListener(), this);
        getServer().getPluginManager().registerEvents(new me.dreamdevs.slender.listeners.ChatListener(), this);
        getServer().getPluginManager().registerEvents(new me.dreamdevs.slender.listeners.FlashlightListener(), this);

        // Init disguise system AFTER all listeners are registered
        // (ProtocolLib must be loaded first)
        Bukkit.getScheduler().runTask(this, DisguiseManager::init);

        this.database.autoSaveData();

        new Metrics(this, 18471);

        if (Config.UPDATE_CHECKER.toBoolean()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                    new UpdateChecker(this, 109730).getVersion(version -> {
                        // Fixed update checker: Only alert if version is truly new and not the legacy one
                        String currentVersion = getPluginMeta().getVersion();
                        if (!currentVersion.equals(version) && !version.startsWith("1.3")) {
                            Util.sendPluginMessage("");
                            Util.sendPluginMessage("&aThere is new SlendermanPlugin version!");
                            Util.sendPluginMessage("&aYour version: " + currentVersion);
                            Util.sendPluginMessage("&aNew version: " + version);
                            Util.sendPluginMessage("");
                        }
                    }), 10L, 20L * 300);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().forEach(world ->
                world.getEntities().stream().filter(Item.class::isInstance).forEach(Entity::remove));

        DisguiseManager.undisguiseAll();

        this.database.saveData();
        this.database.disconnect();
    }

    private void loadConfig() {
        File config = new File(getDataFolder(), "config.yml");

        // Use bundled config.yml as template if file doesn't exist
        if (!config.exists()) {
            saveResource("config.yml", false);
        }

        YamlConfiguration conf = YamlConfiguration.loadConfiguration(config);

        // Merge any missing keys from the bundled template
        InputStream bundled = getResource("config.yml");
        if (bundled != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8));
            conf.setDefaults(defaults);
            conf.options().copyDefaults(true);
        }

        // Also fill from Config enum defaults for any still-missing keys
        Stream.of(Config.values())
                .filter(setting -> conf.getString(setting.getPath()) == null)
                .forEach(setting -> conf.set(setting.getPath(), setting.getDefaultValue()));

        Config.setConfiguration(config);
        try {
            conf.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLang() {
        File config = new File(getDataFolder(), "langauge.yml");
        Util.createFile(config);

        YamlConfiguration conf = YamlConfiguration.loadConfiguration(config);
        Stream.of(Langauge.values())
                .filter(setting -> conf.getString(setting.getPath()) == null)
                .forEach(setting -> conf.set(setting.getPath(), setting.getDefaultMessage()));

        Langauge.setConfiguration(conf);
        try {
            conf.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDefaultArena() {
        File arenasDir = new File(getDataFolder(), "arenas");
        if (!arenasDir.exists()) {
            arenasDir.mkdirs();
        }

        File defaultArena = new File(arenasDir, "default.yml");
        if (!defaultArena.exists()) {
            try (InputStream in = getResource("arenas/default.yml")) {
                if (in != null) {
                    Files.copy(in, defaultArena.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Util.sendPluginMessage("&aDefault arena 'default.yml' created!");
                }
            } catch (IOException e) {
                Util.sendPluginMessage("&cCould not create default arena file.");
            }
        }
    }
}

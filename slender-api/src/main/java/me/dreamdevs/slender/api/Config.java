package me.dreamdevs.slender.api;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Central configuration enum.
 * Every value here maps to a key in config.yml.
 * Defaults are used when the key is missing (e.g. older config files).
 */
public enum Config {

    // ── General ───────────────────────────────────────────────────────────
    LANGUAGE("General.Language", "en"),
    PREFIX("General.Prefix", "&8[&cSlender&8] &r"),
    UPDATE_CHECKER("General.Update-Checker", true),

    // ── Game settings ─────────────────────────────────────────────────────
    PAGES_TO_WIN("GameSettings.Pages-To-Win", 8),
    SLENDERMAN_HEALTH("GameSettings.SlenderMan-Health", 40),
    STARTING_COUNTDOWN("GameSettings.Starting-Countdown", 30),
    ENDING_TIMER("GameSettings.Ending-Timer", 15),
    RESTART_DELAY_TICKS("GameSettings.Restart-Delay-Ticks", 100),

    // ── Torch settings ────────────────────────────────────────────────────
    TORCH_MAX_USES("GameSettings.Torch.Max-Uses", 3),
    TORCH_COOLDOWN_SECONDS("GameSettings.Torch.Cooldown-Seconds", 5),
    TORCH_NIGHT_VISION_TICKS("GameSettings.Torch.Night-Vision-Ticks", 100),

    // ── Atmosphere effects ────────────────────────────────────────────────
    USE_DARKNESS_EFFECT("GameSettings.Atmosphere.Use-Darkness-Effect", true),
    DARKNESS_REAPPLY_INTERVAL("GameSettings.Atmosphere.Darkness-Reapply-Interval-Seconds", 3),
    DARKNESS_DURATION_TICKS("GameSettings.Atmosphere.Darkness-Duration-Ticks", 80),

    // ── Terror radius ─────────────────────────────────────────────────────
    USE_TERROR_RADIUS("GameSettings.TerrorRadius.Enabled", true),
    TERROR_RADIUS("GameSettings.TerrorRadius.Music-Radius", 7),
    NAUSEA_RADIUS("GameSettings.TerrorRadius.Nausea-Radius", 3),
    NAUSEA_DURATION_TICKS("GameSettings.TerrorRadius.Nausea-Duration-Ticks", 60),
    NAUSEA_AMPLIFIER("GameSettings.TerrorRadius.Nausea-Amplifier", 1),

    // ── Disguise / Skins ──────────────────────────────────────────────────
    USE_DISGUISE("GameSettings.Disguise.Enabled", true),
    DEFAULT_DISGUISE("GameSettings.Disguise.Default-Skin", "ENDERMAN"),

    // ── Survivor items ────────────────────────────────────────────────────
    SURVIVOR_WEAPON_MATERIAL("GameSettings.Items.Survivor-Weapon", "WOODEN_SWORD"),
    SLENDERMAN_WEAPON_MATERIAL("GameSettings.Items.SlenderMan-Weapon", "IRON_SWORD"),

    // ── Lobby items (materials) ───────────────────────────────────────────
    ITEM_ARENA_SELECTOR_MATERIAL("LobbyItems.Arena-Selector", "CHEST"),
    ITEM_LEAVE_MATERIAL("LobbyItems.Leave", "RED_BED"),
    ITEM_MY_PROFILE_MATERIAL("LobbyItems.My-Profile", "PLAYER_HEAD"),
    ITEM_PLAY_AGAIN_MATERIAL("LobbyItems.Play-Again", "PAPER"),
    ITEM_PARTY_MENU_MATERIAL("LobbyItems.Party-Menu", "SLIME_BALL"),
    ITEM_SHOP_MATERIAL("LobbyItems.Shop", "GOLD_INGOT"),
    ITEM_SPECTATOR_SETTINGS_MATERIAL("LobbyItems.Spectator-Settings", "STICK"),
    ITEM_SPECTATOR_TELEPORTER_MATERIAL("LobbyItems.Spectator-Teleporter", "COMPASS"),

    // ── Party ─────────────────────────────────────────────────────────────
    USE_PARTY("GameSettings.Party.Enabled", true),
    PARTY_MAX_MEMBERS("GameSettings.Party.Max-Members", 4),

    // ── Database ──────────────────────────────────────────────────────────
    DATABASE_TYPE("Database.Type", "YAML"),
    DATABASE_AUTO_SAVE("Database.Auto-Save", 300),
    DATABASE_HOST("Database.Host", "localhost"),
    DATABASE_PORT("Database.Port", 3306),
    DATABASE_NAME("Database.Name", "slender"),
    DATABASE_USER("Database.User", "root"),
    DATABASE_PASSWORD("Database.Password", "password"),

    // ── Perk cooldowns (seconds) ──────────────────────────────────────────
    PERK_RUNAWAY_COOLDOWN("Perks.Runaway.Cooldown-Seconds", 20),
    PERK_RUNAWAY_DURATION_TICKS("Perks.Runaway.Duration-Ticks", 100),
    PERK_BETTER_TOGETHER_COOLDOWN("Perks.BetterTogether.Cooldown-Seconds", 25),
    PERK_BETTER_TOGETHER_RADIUS("Perks.BetterTogether.Radius-Blocks", 8),
    PERK_ARCHAEOLOGIST_COOLDOWN("Perks.Archaeologist.Cooldown-Seconds", 30),
    PERK_ARCHAEOLOGIST_NIGHT_VISION_TICKS("Perks.Archaeologist.Night-Vision-Ticks", 160),

    // ── Perk prices ──────────────────────────────────────────────────────
    PRICE_RUNAWAY("Prices.Perks.Runaway", 500),
    PRICE_BETTER_TOGETHER("Prices.Perks.BetterTogether", 750),
    PRICE_ARCHAEOLOGIST("Prices.Perks.Archaeologist", 1000),
    PRICE_RESILIENCE("Prices.Perks.Resilience", 600),
    PRICE_TRACKING("Prices.Perks.Tracking", 800),
    PRICE_ECHO("Prices.Perks.Echo", 400),
    PRICE_SPIRIT("Prices.Perks.Spirit", 1200),
    PRICE_PRAYER_SPEED("Prices.Perks.PrayerSpeed", 900),
    PRICE_KILLER_INSTINCT("Prices.Perks.KillerInstinct", 1500),
    PRICE_ENDLESS_AGONY("Prices.Perks.EndlessAgony", 1300),
    PRICE_DARK_ABYSS("Prices.Perks.DarkAbyss", 1100),
    PRICE_FROM_THE_DARK("Prices.Perks.FromTheDark", 1400),
    PRICE_PAGES_BELONGINGS("Prices.Perks.PagesBelongings", 1600);

    // ─────────────────────────────────────────────────────────────────────

    private static YamlConfiguration configuration;
    private final @Getter Object defaultValue;
    private final @Getter String path;

    Config(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public static void setConfiguration(File file) {
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public static YamlConfiguration getConfiguration() {
        return configuration;
    }

    public boolean toBoolean() {
        if (configuration == null) return (boolean) defaultValue;
        return configuration.getBoolean(getPath(), (boolean) defaultValue);
    }

    public int toInt() {
        if (configuration == null) return (int) defaultValue;
        return configuration.getInt(getPath(), (int) defaultValue);
    }

    public long toLong() {
        if (configuration == null) return ((int) defaultValue);
        return configuration.getLong(getPath(), ((int) defaultValue));
    }

    public double toDouble() {
        if (configuration == null) return ((int) defaultValue);
        return configuration.getDouble(getPath(), ((int) defaultValue));
    }

    @Override
    public String toString() {
        if (configuration == null) return String.valueOf(defaultValue);
        String val = configuration.getString(getPath());
        return val != null ? val : String.valueOf(defaultValue);
    }
}

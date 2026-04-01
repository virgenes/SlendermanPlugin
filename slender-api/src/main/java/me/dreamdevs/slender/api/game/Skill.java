package me.dreamdevs.slender.api.game;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum Skill {

    WALK_SPEED("Walk Speed", Material.LEATHER_BOOTS, "&7Increases your base movement speed."),
    STAMINA("Stamina", Material.FEATHER, "&7Increases your sprint duration."),
    RESISTANCE("Resistance", Material.IRON_CHESTPLATE, "&7Reduces damage taken from monsters."),
    COIN_BOOSTER("Coin Booster", Material.GOLD_INGOT, "&7Increases coins earned per match.");

    private final String displayName;
    private final Material icon;
    private final String description;

    Skill(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }
}


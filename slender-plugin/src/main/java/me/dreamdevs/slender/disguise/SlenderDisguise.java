package me.dreamdevs.slender.disguise;

import lombok.Getter;
import me.dreamdevs.slender.api.Config;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Available disguises for the Slenderman role.
 * Uses Bukkit EntityType — no NMS required for the enum itself.
 */
@Getter
public enum SlenderDisguise {

    ENDERMAN      ("Enderman",       EntityType.ENDERMAN,       Material.ENDER_EYE,             0),
    WITHER        ("Wither",         EntityType.WITHER,         Material.WITHER_SKELETON_SKULL,  200),
    PHANTOM       ("Phantom",        EntityType.PHANTOM,        Material.PHANTOM_MEMBRANE,       350),
    RAVAGER       ("Ravager",        EntityType.RAVAGER,        Material.IRON_CHESTPLATE,        500),
    ELDER_GUARDIAN("Elder Guardian", EntityType.ELDER_GUARDIAN, Material.PRISMARINE_CRYSTALS,    750),
    WARDEN        ("Warden",         EntityType.WARDEN,         Material.SCULK_SENSOR,           1000);

    private final String displayName;
    private final EntityType entityType;
    private final Material icon;
    private final int cost;

    SlenderDisguise(String displayName, EntityType entityType, Material icon, int cost) {
        this.displayName = displayName;
        this.entityType = entityType;
        this.icon = icon;
        this.cost = cost;
    }

    /** Returns the disguise for the given name, falling back to ENDERMAN. */
    public static SlenderDisguise fromName(String name) {
        if (name == null) return ENDERMAN;
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception e) {
            return ENDERMAN;
        }
    }

    /** Returns the disguise configured in config.yml, falling back to ENDERMAN. */
    public static SlenderDisguise fromConfig() {
        return fromName(Config.DEFAULT_DISGUISE.toString());
    }
}

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

    ENDERMAN      ("Enderman",       "ENDERMAN",       "ENDER_EYE",             0),
    WITHER        ("Wither",         "WITHER",         "WITHER_SKELETON_SKULL", 200),
    PHANTOM       ("Phantom",        "PHANTOM",        "PHANTOM_MEMBRANE",      350),
    RAVAGER       ("Ravager",        "RAVAGER",        "IRON_CHESTPLATE",       500),
    ELDER_GUARDIAN("Elder Guardian", "ELDER_GUARDIAN", "PRISMARINE_CRYSTALS",   750),
    WARDEN        ("Warden",         "WARDEN",         "SCULK_SENSOR",          1000);

    private final String displayName;
    private final String entityTypeName;
    private final String iconMaterialName;
    private final int cost;

    private EntityType entityTypeCache = null;
    private Material iconCache = null;

    SlenderDisguise(String displayName, String entityTypeName, String iconMaterialName, int cost) {
        this.displayName = displayName;
        this.entityTypeName = entityTypeName;
        this.iconMaterialName = iconMaterialName;
        this.cost = cost;
    }

    public EntityType getEntityType() {
        if (entityTypeCache == null) {
            try {
                entityTypeCache = EntityType.valueOf(entityTypeName);
            } catch (Exception | NoSuchFieldError e) {
                return EntityType.ENDERMAN;
            }
        }
        return entityTypeCache;
    }

    public Material getIcon() {
        if (iconCache == null) {
            try {
                iconCache = Material.valueOf(iconMaterialName);
            } catch (Exception | NoSuchFieldError e) {
                return Material.ENDER_EYE;
            }
        }
        return iconCache;
    }

    public boolean isAvailableInThisVersion() {
        try {
            EntityType.valueOf(entityTypeName);
            Material.valueOf(iconMaterialName);
            return true;
        } catch (Exception | NoSuchFieldError e) {
            return false;
        }
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

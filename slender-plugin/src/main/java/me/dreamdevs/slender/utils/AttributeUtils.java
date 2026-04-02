package me.dreamdevs.slender.utils;

import org.bukkit.attribute.Attribute;

public class AttributeUtils {

    public static Attribute getMaxHealth() {
        try {
            return Attribute.valueOf("MAX_HEALTH");
        } catch (IllegalArgumentException e) {
            return Attribute.valueOf("GENERIC_MAX_HEALTH");
        }
    }

}

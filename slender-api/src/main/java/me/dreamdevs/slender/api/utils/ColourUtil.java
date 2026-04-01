package me.dreamdevs.slender.api.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.stream.Collectors;

public class ColourUtil {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .extractUrls()
            .hexColors()
            .build();

    public static String colorize(String string) {
        if (string == null) return null;
        return LegacyComponentSerializer.legacySection().serialize(colorizeToComponent(string));
    }

    public static Component colorizeToComponent(String string) {
        if (string == null) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(string.replace("\\n", "\n"));
    }

    public static List<Component> colouredLoreToComponents(List<String> lore) {
        return Optional.ofNullable(lore)
                .map(strings -> strings.stream()
                        .map(ColourUtil::colorizeToComponent)
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    public static List<String> colouredLore(String... lore) {
        return Optional.ofNullable(lore).map(l -> colouredLore(Arrays.asList(l))).orElse(new ArrayList<>());
    }

    public static List<String> colouredLore(List<String> lore) {
        return Optional.ofNullable(lore).map(strings -> strings.stream().map(ColourUtil::colorize).collect(Collectors.toList())).orElse(new ArrayList<>());
    }

    public static List<String> colouredLore(String lore) {
        List<String> list = new ArrayList<>();
        if (lore == null) return list;
        String[] strings = lore.split("\n");
        for (String s : strings)
            list.add(colorize(s));
        return list;
    }

}
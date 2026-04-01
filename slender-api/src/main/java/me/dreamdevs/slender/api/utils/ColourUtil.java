package me.dreamdevs.slender.api.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColourUtil {

    public static String colorize(String string) {
        if(string == null) return null;
        // Convert literal \n to actual newlines
        string = string.replace("\\n", "\n");
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        for (Matcher matcher = pattern.matcher(string); matcher.find(); matcher = pattern.matcher(string)) {
            String color = string.substring(matcher.start(), matcher.end());
            string = string.replace(color, ChatColor.of(color) + "");
        }
        string = ChatColor.translateAlternateColorCodes('&', string);
        return string;
    }

    public static List<String> colouredLore(String... lore) {
        return Optional.ofNullable(lore).map(ColourUtil::colouredLore).orElse(new ArrayList<>());
    }

    public static List<String> colouredLore(List<String> lore) {
        return Optional.ofNullable(lore).map(strings -> strings.stream().map(ColourUtil::colorize).collect(Collectors.toList())).orElse(new ArrayList<>());
    }

    public static List<String> colouredLore(String lore) {
        List<String> list = new ArrayList<>();
        String[] strings = lore.split("\n");
        for(String s : strings)
            list.add(colorize(s));
        return list;
    }

    public static String[] colouredArrayLore(String lore) {
        return Stream.of(lore.split("\n")).map(ColourUtil::colorize).toArray(String[]::new);
    }

}
package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.Skill;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillsMenu extends ItemMenu {

    public SkillsMenu(GamePlayer gamePlayer) {
        super(ColourUtil.colorize("&0&lEvolution &8» &b&lSkills"), Size.FIVE_LINE);

        // Border Decoration
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = border.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.empty());
        border.setItemMeta(meta);

        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int s : borderSlots) {
            setItem(s, new MenuItem(" ", border) {
                @Override
                public void onItemClick(ItemClickEvent event) {}
            });
        }

        // Skills
        int slot = 20;
        for (Skill skill : Skill.values()) {
            setItem(slot, new SkillItem(gamePlayer, skill));
            slot += 2;
            if (slot == 26) slot = 28; // Simple centering logic
        }

        // Back button
        setItem(40, new BackItem(gamePlayer));
    }

    private static class SkillItem extends MenuItem {

        private final GamePlayer gamePlayer;
        private final Skill skill;

        public SkillItem(GamePlayer gamePlayer, Skill skill) {
            super(ColourUtil.colorize("&b&l" + skill.getDisplayName()), 
                  new ItemStack(skill.getIcon()), 
                  getLore(gamePlayer, skill));
            this.gamePlayer = gamePlayer;
            this.skill = skill;
        }

        private static String[] getLore(GamePlayer gamePlayer, Skill skill) {
            List<String> lore = new ArrayList<>();
            int level = gamePlayer.getSkillLevel(skill);
            lore.add(ColourUtil.colorize(skill.getDescription()));
            lore.add("");
            lore.add(ColourUtil.colorize("&7Current Level: &b" + level + "/5"));
            
            double currentBonus = level * 5.0; // Simple example: 5% per level
            lore.add(ColourUtil.colorize("&7Current Bonus: &a+" + currentBonus + "%"));
            lore.add("");
            
            if (level < 5) {
                int cost = (level + 1) * 100;
                lore.add(ColourUtil.colorize("&6Upgrade Cost: &e" + cost + " Coins"));
                lore.add("");
                lore.add(ColourUtil.colorize("&eClick to upgrade!"));
            } else {
                lore.add(ColourUtil.colorize("&6&lMAX LEVEL"));
            }
            
            return lore.toArray(new String[0]);
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            int level = gamePlayer.getSkillLevel(skill);
            if (level >= 5) {
                event.getPlayer().sendMessage(ColourUtil.colorize("&cThis skill is already at max level!"));
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int cost = (level + 1) * 100;
            int coins = gamePlayer.getStatistic(Statistic.COINS);

            if (coins >= cost) {
                gamePlayer.setStatistic(Statistic.COINS, coins - cost);
                gamePlayer.setSkillLevel(skill, level + 1);
                event.getPlayer().sendMessage(ColourUtil.colorize("&a&lUPGRADED! &b" + skill.getDisplayName() + " &7is now level &f" + (level + 1)));
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                
                // Refresh menu
                new SkillsMenu(gamePlayer).open(event.getPlayer());
            } else {
                event.getPlayer().sendMessage(ColourUtil.colorize("&cYou don't have enough coins! &7(Need " + cost + ")"));
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private static class BackItem extends MenuItem {
        public BackItem(GamePlayer gamePlayer) {
            super(ColourUtil.colorize("&c&lBack to Profile"), new ItemStack(Material.BARRIER));
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            new MyProfileMenu(event.getPlayer()).open(event.getPlayer());
        }
    }
}

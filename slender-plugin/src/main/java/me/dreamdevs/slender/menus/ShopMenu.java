package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ShopMenu extends ItemMenu {

    public ShopMenu(GamePlayer gamePlayer) {
        super(ColourUtil.colorize("&6&lShop"), Size.TWO_LINE);

        setItem(12, new ShopSectionItem(
                ColourUtil.colorize("&a&lSurvivor Perks"),
                new ItemStack(Material.IRON_SWORD),
                Arrays.asList(
                        ColourUtil.colorize("&7Select your survivor perks"),
                        "",
                        ColourUtil.colorize("&eClick to open")
                ),
                () -> {
                    new SurvivorPerkMenu(gamePlayer).open(gamePlayer.getPlayer());
                }
        ));

        setItem(14, new ShopSectionItem(
                ColourUtil.colorize("&c&lMonster Shop"),
                new ItemStack(Material.NETHER_STAR),
                Arrays.asList(
                        ColourUtil.colorize("&7Monster perks & skins"),
                        "",
                        ColourUtil.colorize("&eClick to open")
                ),
                () -> {
                    new MonsterShopMenu(gamePlayer).open(gamePlayer.getPlayer());
                }
        ));
    }

    private static class ShopSectionItem extends MenuItem {

        private final Runnable action;

        public ShopSectionItem(String name, ItemStack icon, java.util.List<String> lore, Runnable action) {
            super(name, icon, lore.toArray(new String[0]));
            this.action = action;
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), action, 4L);
        }
    }
}

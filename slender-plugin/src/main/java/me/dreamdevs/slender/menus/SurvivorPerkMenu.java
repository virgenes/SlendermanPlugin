package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SurvivorPerkMenu extends ItemMenu {

    public SurvivorPerkMenu(GamePlayer gamePlayer) {
        super(Langauge.MENU_PERKS_TITLE.toString(), Size.THREE_LINE);

        List<Perk> perks = SlenderMain.getInstance().getPerkManager().getPerksByRole(Role.SURVIVOR);
        int slot = 10;
        for (Perk perk : perks) {
            PerkInfo info = perk.getClass().getAnnotation(PerkInfo.class);
            setItem(slot, new SelectPerkItem(Role.SURVIVOR, perk, info));
            slot++;
            if (slot == 17) slot = 19;
        }

        setItem(26, new BackItem(gamePlayer));
    }

    private static class SelectPerkItem extends MenuItem {

        private final Perk perk;
        private final Role role;
        private final String perkName;

        public SelectPerkItem(Role role, Perk perk, PerkInfo info) {
            super(info.name(), new ItemStack(info.icon()));
            this.perk = perk;
            this.role = role;
            this.perkName = info.name();
        }

        @Override
        public ItemStack getFinalIcon(org.bukkit.entity.Player player) {
            GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
            ItemStack item = super.getIcon().clone();
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();

            // Header
            lore.add(net.kyori.adventure.text.Component.text(""));

            // Original Lore
            for (String line : perk.getLore()) {
                lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent(line));
            }

            lore.add(net.kyori.adventure.text.Component.text(""));

            if (gp != null) {
                if (gp.getPerk(role) != null && gp.getPerk(role).getClass().equals(perk.getClass())) {
                    lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&b&l[EQUIPPED]"));
                } else if (gp.ownsPerk(perkName)) {
                    lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&a&l[OWNED]"));
                    lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&7Click to select"));
                } else {
                    int price = getPrice(perkName);
                    lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&6Price: " + price + " coins"));
                    lore.add(me.dreamdevs.slender.api.utils.ColourUtil.colorizeToComponent("&7Click to purchase"));
                }
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        private int getPrice(String name) {
            try {
                me.dreamdevs.slender.api.Config config = me.dreamdevs.slender.api.Config.valueOf("PRICE_" + name.toUpperCase().replace(" ", "_"));
                return config.toInt();
            } catch (Exception e) {
                return 1000; // Default price
            }
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            Player player = event.getPlayer();
            GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
            if (gp == null) return;

            if (gp.getPerk(role) != null && gp.getPerk(role).getClass().equals(perk.getClass())) {
                player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorize("&cThis perk is already equipped!"));
                return;
            }

            if (gp.ownsPerk(perkName)) {
                // Select
                gp.setPerk(role, perk);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.sendMessage(Langauge.PERKS_SELECTED.toString()
                        .replace("%PERK_NAME%", perkName)
                        .replace("%TEAM%", Langauge.ARENA_SURVIVOR_TEAM.toString()));
                event.setWillUpdate(true);
            } else {
                // Purchase
                int price = getPrice(perkName);
                int balance = gp.getStatistic(me.dreamdevs.slender.api.Statistic.COINS);

                if (balance >= price) {
                    gp.setStatistic(me.dreamdevs.slender.api.Statistic.COINS, balance - price);
                    gp.unlockPerk(perkName);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorize("&aYou purchased the perk &e" + perkName + " &afor &6" + price + " coins&a!"));
                    event.setWillUpdate(true);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    player.sendMessage(me.dreamdevs.slender.api.utils.ColourUtil.colorize("&cYou don't have enough coins! Only (" + balance + "/" + price + ")"));
                }
            }
        }
    }

    private static class BackItem extends MenuItem {

        private final GamePlayer gamePlayer;

        public BackItem(GamePlayer gamePlayer) {
            super(Langauge.MENU_BACK_ITEM_NAME.toString(), new ItemStack(Material.BARRIER));
            this.gamePlayer = gamePlayer;
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
                new ShopMenu(gamePlayer).open(event.getPlayer());
            }, 4L);
        }
    }
}

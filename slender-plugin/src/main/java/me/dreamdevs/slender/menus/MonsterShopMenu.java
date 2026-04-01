package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class MonsterShopMenu extends ItemMenu {

    public MonsterShopMenu(GamePlayer gamePlayer) {
        super(ColourUtil.colorize("&c&lMonster Shop"), Size.FIVE_LINE);

        // Perks row (top row, slots 10-14)
        int slot = 10;
        for (Perk perk : SlenderMain.getInstance().getPerkManager().getPerksByRole(Role.SLENDER)) {
            PerkInfo info = perk.getClass().getAnnotation(PerkInfo.class);
            setItem(slot, new MonsterPerkItem(gamePlayer, perk, info));
            slot++;
        }

        // Separator row (row 3, slots 18-26)
        for (int i = 18; i <= 26; i++) {
            setItem(i, new SeparatorItem());
        }

        // Skins row 1 (row 4, slots 28-34)
        slot = 28;
        List<SlenderDisguise> skins = Arrays.asList(SlenderDisguise.values());
        for (int i = 0; i < Math.min(7, skins.size()); i++) {
            setItem(slot, new SkinItem(gamePlayer, skins.get(i)));
            slot++;
        }

        // Skins row 2 (row 5, slots 37-43) if more than 7 skins
        if (skins.size() > 7) {
            slot = 37;
            for (int i = 7; i < skins.size(); i++) {
                setItem(slot, new SkinItem(gamePlayer, skins.get(i)));
                slot++;
            }
        }

        // Back button (bottom right)
        setItem(44, new BackItem(gamePlayer));
    }

    private static class MonsterPerkItem extends MenuItem {

        private final Perk perk;
        private final String perkName;

        public MonsterPerkItem(GamePlayer gamePlayer, Perk perk, PerkInfo info) {
            super(ColourUtil.colorize("&c" + info.name()), new ItemStack(info.icon()));
            this.perk = perk;
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
                lore.add(ColourUtil.colorizeToComponent(line));
            }

            lore.add(net.kyori.adventure.text.Component.text(""));

            if (gp != null) {
                if (gp.getPerk(Role.SLENDER) != null && gp.getPerk(Role.SLENDER).getClass().equals(perk.getClass())) {
                    lore.add(ColourUtil.colorizeToComponent("&b&l[EQUIPPED]"));
                } else if (gp.ownsPerk(perkName)) {
                    lore.add(ColourUtil.colorizeToComponent("&a&l[OWNED]"));
                    lore.add(ColourUtil.colorizeToComponent("&7Click to select"));
                } else {
                    int price = getPrice(perkName);
                    lore.add(ColourUtil.colorizeToComponent("&6Price: " + price + " coins"));
                    lore.add(ColourUtil.colorizeToComponent("&7Click to purchase"));
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

            if (gp.getPerk(Role.SLENDER) != null && gp.getPerk(Role.SLENDER).getClass().equals(perk.getClass())) {
                player.sendMessage(ColourUtil.colorize("&cThis perk is already equipped!"));
                return;
            }

            if (gp.ownsPerk(perkName)) {
                // Select
                gp.setPerk(Role.SLENDER, perk);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.sendMessage(ColourUtil.colorize("&aSelected perk: &c" + perkName));
                event.setWillUpdate(true);
            } else {
                // Purchase
                int price = getPrice(perkName);
                int balance = gp.getStatistic(me.dreamdevs.slender.api.Statistic.COINS);

                if (balance >= price) {
                    gp.setStatistic(me.dreamdevs.slender.api.Statistic.COINS, balance - price);
                    gp.unlockPerk(perkName);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    player.sendMessage(ColourUtil.colorize("&aYou purchased the perk &e" + perkName + " &afor &6" + price + " coins&a!"));
                    event.setWillUpdate(true);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    player.sendMessage(ColourUtil.colorize("&cYou don't have enough coins! Only (" + balance + "/" + price + ")"));
                }
            }
        }
    }

    private static class SkinItem extends MenuItem {

        private final GamePlayer gamePlayer;
        private final SlenderDisguise skin;

        public SkinItem(GamePlayer gamePlayer, SlenderDisguise skin) {
            super(ColourUtil.colorize("&e" + skin.getDisplayName()), new ItemStack(skin.getIcon()),
                    buildLore(gamePlayer, skin));
            this.gamePlayer = gamePlayer;
            this.skin = skin;
        }

        private static String[] buildLore(GamePlayer gp, SlenderDisguise s) {
            if (gp.getEquippedSkin() == s) {
                return new String[]{"", ColourUtil.colorize("&b&lEquipped")};
            }
            if (gp.ownsSkin(s)) {
                return new String[]{"", ColourUtil.colorize("&aOwned"), "", ColourUtil.colorize("&7Click to equip")};
            }
            return new String[]{
                    "",
                    ColourUtil.colorize("&cLocked &7- &6" + s.getCost() + " coins"),
                    "",
                    ColourUtil.colorize("&7Click to purchase")
            };
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            if (gamePlayer.ownsSkin(skin)) {
                gamePlayer.equipSkin(skin);
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.getPlayer().sendMessage(ColourUtil.colorize("&aSkin &e" + skin.getDisplayName() + " &aequipped!"));
                if (gamePlayer.isInArena()) {
                    Player p = gamePlayer.getPlayer();
                    if (p != null) DisguiseManager.disguise(p, skin);
                }
            } else {
                int coins = gamePlayer.getStatistic(Statistic.COINS);
                if (coins < skin.getCost()) {
                    event.getPlayer().sendMessage(ColourUtil.colorize("&cNot enough coins! You need &6" + skin.getCost() + " &ccoins."));
                    return;
                }
                gamePlayer.setStatistic(Statistic.COINS, coins - skin.getCost());
                gamePlayer.purchaseSkin(skin);
                gamePlayer.equipSkin(skin);
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.getPlayer().sendMessage(ColourUtil.colorize("&aSkin &e" + skin.getDisplayName() + " &apurchased!"));
                if (gamePlayer.isInArena()) {
                    Player p = gamePlayer.getPlayer();
                    if (p != null) DisguiseManager.disguise(p, skin);
                }
            }
        }
    }

    private static class SeparatorItem extends MenuItem {
        public SeparatorItem() {
            super(" ", new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
        }
    }

    private static class BackItem extends MenuItem {

        private final GamePlayer gamePlayer;

        public BackItem(GamePlayer gamePlayer) {
            super(ColourUtil.colorize("&cBack"), new ItemStack(Material.BARRIER));
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

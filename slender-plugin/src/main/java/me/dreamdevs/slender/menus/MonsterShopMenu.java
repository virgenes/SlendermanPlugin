package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
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

        public MonsterPerkItem(GamePlayer gamePlayer, Perk perk, PerkInfo info) {
            super(ColourUtil.colorize("&c" + info.name()), new ItemStack(info.icon()),
                    ColourUtil.colouredLore(perk.getLore()).toArray(new String[0]));
            this.perk = perk;
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
            if (gp != null) {
                gp.setPerk(Role.SLENDER, perk);
            }
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            event.getPlayer().sendMessage(ColourUtil.colorize("&aSelected perk: &c" +
                    perk.getClass().getAnnotation(PerkInfo.class).name()));
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

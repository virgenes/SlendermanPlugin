package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.disguise.SlenderDisguise;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class SlenderShopMenu extends ItemMenu {

    public SlenderShopMenu(GamePlayer gamePlayer) {
        super(Langauge.SHOP_TITLE.toString(), Size.FOUR_LINE);

        int slot = 10;
        for (SlenderDisguise skin : SlenderDisguise.values()) {
            setItem(slot, new SkinItem(gamePlayer, skin));
            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
        }
    }

    private static class SkinItem extends MenuItem {

        private final GamePlayer gamePlayer;
        private final SlenderDisguise skin;

        public SkinItem(GamePlayer gamePlayer, SlenderDisguise skin) {
            super(ColourUtil.colorize("&e" + skin.getDisplayName()), new ItemStack(skin.getIcon()),
                    buildLore(gamePlayer, skin).toArray(String[]::new));
            this.gamePlayer = gamePlayer;
            this.skin = skin;
        }

        private static java.util.List<String> buildLore(GamePlayer gamePlayer, SlenderDisguise skin) {
            if (gamePlayer.getEquippedSkin() == skin) {
                return ColourUtil.colouredLore(Arrays.asList("", Langauge.SHOP_SKIN_EQUIPPED.toString()));
            }
            if (gamePlayer.ownsSkin(skin)) {
                return ColourUtil.colouredLore(Arrays.asList("", "&aOwned", "", "&7Click to equip"));
            }
            return ColourUtil.colouredLore(Arrays.asList(
                    "",
                    Langauge.SHOP_SKIN_LOCKED.toString().replace("%COST%", String.valueOf(skin.getCost())),
                    "",
                    "&7Click to purchase"
            ));
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            if (gamePlayer.ownsSkin(skin)) {
                gamePlayer.equipSkin(skin);
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.getPlayer().sendMessage(ColourUtil.colorize(Langauge.SHOP_SKIN_EQUIPPED_MSG.toString()
                        .replace("%SKIN%", skin.getDisplayName())));
                if (gamePlayer.isInArena()) {
                    Player p = gamePlayer.getPlayer();
                    if (p != null) {
                        DisguiseManager.disguise(p, skin);
                    }
                }
            } else {
                int coins = gamePlayer.getStatistic(Statistic.COINS);
                if (coins < skin.getCost()) {
                    event.getPlayer().sendMessage(ColourUtil.colorize(Langauge.SHOP_NOT_ENOUGH_COINS.toString()
                            .replace("%COST%", String.valueOf(skin.getCost()))));
                    return;
                }
                gamePlayer.setStatistic(Statistic.COINS, coins - skin.getCost());
                gamePlayer.purchaseSkin(skin);
                gamePlayer.equipSkin(skin);
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.getPlayer().sendMessage(ColourUtil.colorize(Langauge.SHOP_SKIN_PURCHASED.toString()
                        .replace("%SKIN%", skin.getDisplayName())));
                if (gamePlayer.isInArena()) {
                    Player p = gamePlayer.getPlayer();
                    if (p != null) {
                        DisguiseManager.disguise(p, skin);
                    }
                }
            }
        }
    }
}

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

        public SelectPerkItem(Role role, Perk perk, PerkInfo info) {
            super(info.name(), new ItemStack(info.icon()), perk.getLore().toArray(String[]::new));
            this.perk = perk;
            this.role = role;
        }

        @Override
        public void onItemClick(ItemClickEvent event) {
            event.setWillClose(true);
            GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(event.getPlayer());
            if (gamePlayer != null) {
                gamePlayer.setPerk(role, perk);
            }
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            event.getPlayer().sendMessage(Langauge.PERKS_SELECTED.toString()
                    .replace("%PERK_NAME%", perk.getClass().getAnnotation(PerkInfo.class).name())
                    .replace("%TEAM%", Langauge.ARENA_SURVIVOR_TEAM.toString()));
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

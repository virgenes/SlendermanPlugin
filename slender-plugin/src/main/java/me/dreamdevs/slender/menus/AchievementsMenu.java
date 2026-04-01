package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AchievementsMenu extends ItemMenu {

    public AchievementsMenu(GamePlayer gamePlayer) {
        super(ColourUtil.colorize("&0&lEvolution &8» &e&lAchievements"), Size.SIX_LINE);

        // Decoration
        ItemStack border = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = border.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.empty());
        border.setItemMeta(meta);

        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int s : borderSlots) {
            setItem(s, new MenuItem(" ", border) {
                @Override
                public void onItemClick(ItemClickEvent event) {}
            });
        }

        // Stats summary
        setItem(4, new MenuItem(ColourUtil.colorize("&e&lLifetime Achievements"), new ItemStack(Material.NETHER_STAR), 
                ColourUtil.colorize("&7Total Wins: &f" + gamePlayer.getStatistic(Statistic.WINS)),
                ColourUtil.colorize("&7Pages Collected: &f" + gamePlayer.getStatistic(Statistic.COLLECTED_PAGES)),
                ColourUtil.colorize("&7Survivors Killed: &f" + gamePlayer.getStatistic(Statistic.KILLED_SURVIVORS)),
                ColourUtil.colorize("&7SlenderMen Killed: &f" + gamePlayer.getStatistic(Statistic.KILLED_SLENDERMEN))) {
            @Override
            public void onItemClick(ItemClickEvent event) {}
        });

        // Some sample achievements
        addAchievement(gamePlayer, 20, "Survivor I", "Win 1 match as Survivor.", Statistic.WINS, 1, Material.LEATHER_BOOTS);
        addAchievement(gamePlayer, 21, "Survivor II", "Win 10 matches as Survivor.", Statistic.WINS, 10, Material.IRON_BOOTS);
        addAchievement(gamePlayer, 22, "Survivor III", "Win 50 matches as Survivor.", Statistic.WINS, 50, Material.DIAMOND_BOOTS);

        addAchievement(gamePlayer, 29, "Hunter I", "Kill 5 Survivors.", Statistic.KILLED_SURVIVORS, 5, Material.STONE_SWORD);
        addAchievement(gamePlayer, 30, "Hunter II", "Kill 25 Survivors.", Statistic.KILLED_SURVIVORS, 25, Material.IRON_SWORD);
        addAchievement(gamePlayer, 31, "Hunter III", "Kill 100 Survivors.", Statistic.KILLED_SURVIVORS, 100, Material.DIAMOND_SWORD);

        addAchievement(gamePlayer, 38, "Scholar I", "Collect 8 pages in one match.", Statistic.COLLECTED_PAGES, 8, Material.PAPER); // Simple check
        addAchievement(gamePlayer, 39, "Scholar II", "Collect 40 pages total.", Statistic.COLLECTED_PAGES, 40, Material.BOOK);
        addAchievement(gamePlayer, 40, "Scholar III", "Collect 200 pages total.", Statistic.COLLECTED_PAGES, 200, Material.WRITTEN_BOOK);

        // Back button
        setItem(49, new MenuItem(ColourUtil.colorize("&c&lBack to Profile"), new ItemStack(Material.BARRIER)) {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                new MyProfileMenu(event.getPlayer()).open(event.getPlayer());
            }
        });
    }

    private void addAchievement(GamePlayer p, int slot, String name, String desc, Statistic stat, int req, Material mat) {
        boolean unlocked = p.getStatistic(stat) >= req;
        setItem(slot, new MenuItem(ColourUtil.colorize((unlocked ? "&a&l" : "&c&l") + name), 
                new ItemStack(unlocked ? mat : Material.COAL),
                ColourUtil.colorize("&7" + desc),
                "",
                unlocked ? ColourUtil.colorize("&a&lUNLOCKED") : ColourUtil.colorize("&c&lPROGRESS: &f" + p.getStatistic(stat) + "/" + req)) {
            @Override
            public void onItemClick(ItemClickEvent event) {}
        });
    }
}

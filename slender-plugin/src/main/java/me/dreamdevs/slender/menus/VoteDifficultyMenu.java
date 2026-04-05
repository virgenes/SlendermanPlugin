package me.dreamdevs.slender.menus;

import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.game.Difficulty;
import me.dreamdevs.slender.api.inventory.ItemMenu;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class VoteDifficultyMenu extends ItemMenu {

    public VoteDifficultyMenu(Arena arena) {
        super("Vote Difficulty", Size.THREE_LINE);

        setItem(10, new MenuItem("&aEasy (Classic)", new ItemStack(Material.LIME_DYE), 
                "&7Normal Slenderman experience.") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                arena.getDifficultyVotes().put(event.getPlayer().getUniqueId(), Difficulty.EASY);
                event.getPlayer().sendMessage("§aYou voted for Easy Difficulty!");
            }
        });

        setItem(13, new MenuItem("&eIntermediate (Paranoia)", new ItemStack(Material.ORANGE_DYE), 
                "&7Adds fake sounds, fake explosions,",
                "&7and hallucinations to scare you.") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                arena.getDifficultyVotes().put(event.getPlayer().getUniqueId(), Difficulty.INTERMEDIATE);
                event.getPlayer().sendMessage("§eYou voted for Intermediate Difficulty!");
            }
        });

        setItem(16, new MenuItem("&4Hard (Nightmare)", new ItemStack(Material.RED_DYE), 
                "&7Paranoia effects, plus tripping,",
                "&7reduced vision, and NO PERKS allowed!") {
            @Override
            public void onItemClick(ItemClickEvent event) {
                event.setWillClose(true);
                arena.getDifficultyVotes().put(event.getPlayer().getUniqueId(), Difficulty.HARD);
                event.getPlayer().sendMessage("§4You voted for Hard Difficulty!");
            }
        });
    }
}

package me.dreamdevs.slender.game;

import lombok.Getter;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.utils.ColourUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

@Getter
public enum CustomItem {

    ARENA_SELECTOR(Material.CHEST, Langauge.ITEMS_ARENA_SELECTOR_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_ARENA_SELECTOR_DISPLAY_LORE.toString())),

    LEAVE(Material.RED_BED, Langauge.ITEMS_LEAVE_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_LEAVE_DISPLAY_LORE.toString())),

    MY_PROFILE(Material.PLAYER_HEAD, Langauge.ITEMS_MY_PROFILE_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_MY_PROFILE_DISPLAY_LORE.toString())),

    PLAY_AGAIN(Material.PAPER, Langauge.ITEMS_PLAY_AGAIN_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_PLAY_AGAIN_DISPLAY_LORE.toString())),

    PARTY_MENU(Material.SLIME_BALL, Langauge.ITEMS_PARTY_MENU_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_PARTY_MENU_DISPLAY_LORE.toString())),

    PERKS(Material.FEATHER, Langauge.ITEMS_PERKS_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_PERKS_DISPLAY_LORE.toString())),

    SHOP(Material.EMERALD, Langauge.ITEMS_SHOP_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SHOP_DISPLAY_LORE.toString())),

    SPECTATOR_SETTINGS(Material.STICK, Langauge.ITEMS_SPECTATOR_SETTINGS_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SPECTATOR_SETTINGS_DISPLAY_LORE.toString())),

    SPECTATOR_TELEPORTER(Material.COMPASS, Langauge.ITEMS_SPECTATOR_TELEPORT_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SPECTATOR_TELEPORT_DISPLAY_LORE.toString())),

    SURVIVOR_MAP(Material.FILLED_MAP, Langauge.ITEMS_SURVIVOR_MAP_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SURVIVOR_MAP_DISPLAY_LORE.toString())),

    VOTE_MODE(Material.NETHER_STAR, Langauge.ITEMS_VOTE_MODE_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_VOTE_MODE_DISPLAY_LORE.toString())),

    VOTE_DIFFICULTY(Material.CLOCK, Langauge.ITEMS_VOTE_DIFFICULTY_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_VOTE_DIFFICULTY_DISPLAY_LORE.toString())),

    SURVIVOR_WEAPON(Material.WOODEN_SWORD, Langauge.ITEMS_SURVIVOR_SWORD_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SURVIVOR_SWORD_DISPLAY_LORE.toString())),



    SURVIVOR_PERK_ITEM(Material.BLAZE_POWDER, "&6Perk Ability",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click to activate", "&7Your equipped perk"))),

    // SlenderMan's items
    SLENDERMAN_WEAPON(Material.DIAMOND_SWORD, Langauge.ITEMS_SLENDERMAN_SWORD_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SLENDERMAN_SWORD_DISPLAY_LORE.toString())),

    SLENDERMAN_COMPASS(Material.COMPASS, Langauge.ITEMS_SLENDERMAN_COMPASS_DISPLAY_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_SLENDERMAN_COMPASS_DISPLAY_LORE.toString())),

    SLENDERMAN_RADAR(Material.CLOCK, "&cRadar",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click to scan for survivors", "&7Cooldown: %COOLDOWN%s"))),

    SLENDERMAN_PERK_ITEM(Material.BLAZE_ROD, "&4Perk Ability",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click to activate", "&7Your equipped perk"))),

    FORCED_START(Material.GOLD_INGOT, Langauge.ITEMS_FORCED_START_NAME.toString(),
            ColourUtil.colouredLore(Langauge.ITEMS_FORCED_START_LORE.toString())),

    // Phase 6: Escape Room Editor Tools
    ER_GENERATOR_TOOL(Material.DAYLIGHT_DETECTOR, "&e&lTool: Place Generator",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click a block to", "&7set a Generator location."))),
            
    ER_KEY_TOOL(Material.TRIPWIRE_HOOK, "&b&lTool: Place Master Key",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click a block to", "&7set the Master Key spawn."))),

    ER_ESCAPE_TOOL(Material.IRON_DOOR, "&a&lTool: Set Escape Point",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click a block to", "&7set the final Escape Point."))),

    ER_SLENDER_TOOL(Material.REDSTONE_BLOCK, "&c&lTool: Set Slender Spawn",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click a block to", "&7set SlenderMan's spawn."))),

    ER_SURVIVOR_TOOL(Material.BEACON, "&d&lTool: Add Survivor Spawn",
            ColourUtil.colouredLore(Arrays.asList("&7Right-click a block to", "&7add a Survivor spawn."))),

    // Phase 6: Gameplay Items
    ER_MASTER_KEY(Material.TRIPWIRE_HOOK, "&b&lMaster Key",
            ColourUtil.colouredLore(Arrays.asList("&7Use this to open the", "&7Escape Point!"))),

    SLENDER_TRAP(Material.SCAFFOLDING, "&c&lSlender Trap",
            ColourUtil.colouredLore(Arrays.asList("&7Place this to trap", "&7survivors!"))),

    ESCAPE_NOTE(Material.PAPER, "&f&lMysterious Note",
            ColourUtil.colouredLore(Arrays.asList("&7A strange note found", "&7in the darkness.")));

    private final Component displayName;
    private final Material material;
    private final List<Component> lore;

    CustomItem(Material material, String displayName, List<String> lore) {
        this.material = material;
        this.displayName = ColourUtil.colorizeToComponent(displayName);
        this.lore = ColourUtil.colouredLoreToComponents(lore);
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(displayName);
        itemMeta.lore(lore);
        itemMeta.setUnbreakable(true);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

}
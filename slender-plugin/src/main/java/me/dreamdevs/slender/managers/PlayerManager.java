package me.dreamdevs.slender.managers;

import lombok.Getter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.disguise.DisguiseManager;
import me.dreamdevs.slender.game.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PlayerManager {

    private final List<GamePlayer> players;

    public PlayerManager() {
        this.players = new ArrayList<>();
    }

    public GamePlayer getPlayer(@NotNull OfflinePlayer player) {
        return players.stream()
                .filter(gamePlayer -> gamePlayer.getOfflinePlayer().equals(player))
                .findAny()
                .orElse(null);
    }

    public void loadLobby(@NotNull Player player) {
        player.getInventory().setItem(0, CustomItem.ARENA_SELECTOR.toItemStack());
        player.getInventory().setItem(3, CustomItem.PARTY_MENU.toItemStack());
        player.getInventory().setItem(5, CustomItem.SHOP.toItemStack());

        ItemStack itemStack = CustomItem.MY_PROFILE.toItemStack();
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwningPlayer(player);
        itemStack.setItemMeta(skullMeta);

        player.getInventory().setItem(4, itemStack);
    }

    public void sendToLobby(@NotNull Player player) {
        SlenderMain.getInstance().getLobby().teleportPlayerToLobby(player);
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        gamePlayer.clearInventory();
        player.setAllowFlight(false);
        player.setLevel(gamePlayer.getStatistic(Statistic.LEVEL));
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setFoodLevel(20);
        player.setRespawnLocation(null, true);
        player.setExp(0);
        player.setGlowing(false);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.getAttribute(me.dreamdevs.slender.utils.AttributeUtils.getMaxHealth()).setBaseValue(20);
        player.setHealth(20);
        player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            if(SlenderMain.getInstance().getGameManager().isInArena(onlinePlayer)) {
                onlinePlayer.hidePlayer(SlenderMain.getInstance(), player);
                player.hidePlayer(SlenderMain.getInstance(), onlinePlayer);
            } else {
                onlinePlayer.showPlayer(SlenderMain.getInstance(), player);
                player.showPlayer(SlenderMain.getInstance(), onlinePlayer);
            }
        });

        DisguiseManager.undisguise(player);
        loadLobby(player);
        
        // Reapply walk speed multiplier
        me.dreamdevs.slender.listeners.PlayerSkillListener.applyWalkSpeed(player);
    }

}
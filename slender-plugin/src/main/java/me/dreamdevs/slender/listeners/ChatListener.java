package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.utils.RankUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!Config.CHAT_ENABLED.toBoolean()) return;

        Player player = event.getPlayer();
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        
        if (gamePlayer == null) return;

        int level = gamePlayer.getStatistic(Statistic.LEVEL);
        String rank = RankUtils.getRank(level);
        
        String format = Config.CHAT_FORMAT.toString()
                .replace("%RANK%", rank)
                .replace("%PLAYER%", player.getName())
                .replace("%LEVEL%", String.valueOf(level))
                .replace("%MESSAGE%", event.getMessage());

        event.setFormat(ColourUtil.colorize(format.replace("%", "%%"))); 
        // Note: AsyncPlayerChatEvent uses String.format, so we must escape % in the message if we use event.setFormat
        // Actually, it's safer to just cancel and broadcast if we want full control, 
        // but setFormat is standard for simple plugins.
        // Format should be like: "prefix %1$s: %2$s" -> where %1 is name and %2 is message.
        // But the user wants a very specific format. 
        
        // Let's use a better approach for the format to avoid String.format issues:
        String finalMessage = ColourUtil.colorize(format);
        event.setCancelled(true);
        player.getWorld().getPlayers().forEach(p -> p.sendMessage(finalMessage));
        // Or global broadcast if it's a dedicated server:
        // org.bukkit.Bukkit.broadcastMessage(finalMessage);
    }
}

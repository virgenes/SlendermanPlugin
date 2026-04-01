package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.Skill;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerSkillListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        applyWalkSpeed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        applyWalkSpeed(event.getPlayer());
    }

    public static void applyWalkSpeed(Player player) {
        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gp == null) return;

        int level = gp.getSkillLevel(Skill.WALK_SPEED);
        // Default walk speed is 0.2f. Each level adds 0.01f (5% of 0.2)
        float bonus = level * 0.01f;
        player.setWalkSpeed(0.2f + bonus);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(victim);
        if (gp == null) return;

        int resistanceLevel = gp.getSkillLevel(Skill.RESISTANCE);
        if (resistanceLevel > 0) {
            // Each level reduces damage by 5%
            double reduction = 1.0 - (resistanceLevel * 0.05);
            event.setDamage(event.getDamage() * reduction);
        }
    }
}

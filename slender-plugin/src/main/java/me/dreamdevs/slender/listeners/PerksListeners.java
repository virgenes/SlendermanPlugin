package me.dreamdevs.slender.listeners;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.game.perks.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PerksListeners implements Listener {

    // Passive perks that trigger automatically on events:
    // - Spirit: on death, blesses allies
    // - EndlessAgony: on kill, gains token
    // - KillerInstinct: on hit, victim glows
    // - FromTheDark: on hit, slows nearby survivors
    // - DarkAbyss: on kill, toggles blindness aura
    // - PagesBelongings: on page pickup, gains speed
    // - Resilience: passive sanity drain reduction (handled in SanityManager)
    // - Echo: passive detection range increase (handled in TerrorRadiusManager)
    // - PrayerSpeed: passive page collection speed (handled in GameListeners)

    @EventHandler
    public void onSlenderHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        GamePlayer attacker = SlenderMain.getInstance().getPlayerManager().getPlayer(damager);
        if (attacker == null || !attacker.isInArena()) return;

        Arena arena = (Arena) attacker.getArena();
        if (arena == null || arena.getPlayers().get(damager) != Role.SLENDER) return;

        Perk perk = attacker.getPerk(Role.SLENDER);
        if (perk == null) return;

        if (perk instanceof FromTheDark) {
            FromTheDark ftd = SlenderMain.getInstance().getPerkManager().getFromTheDark();
            if (ftd != null) ftd.applyEffect(damager, victim);
        }

        if (perk instanceof KillerInstinct) {
            victim.setGlowing(true);
            SlenderMain.getInstance().getServer().getScheduler().runTaskLater(
                    SlenderMain.getInstance(), () -> victim.setGlowing(false), 100L);
        }

        if (perk instanceof EndlessAgony) {
            EndlessAgony ea = SlenderMain.getInstance().getPerkManager().getEndlessAgony();
            if (ea != null) {
                double bonus = ea.getBonusDamage(damager);
                event.setDamage(event.getDamage() + bonus);
            }
        }
    }

    @EventHandler
    public void onSlenderKill(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        GamePlayer deadPlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(dead);
        if (deadPlayer == null || !deadPlayer.isInArena()) return;

        Arena arena = (Arena) deadPlayer.getArena();
        if (arena == null || arena.getSlenderMan() == null) return;

        Player slenderMan = arena.getSlenderMan();
        GamePlayer slenderGP = SlenderMain.getInstance().getPlayerManager().getPlayer(slenderMan);
        if (slenderGP == null) return;

        Perk perk = slenderGP.getPerk(Role.SLENDER);
        if (perk instanceof EndlessAgony) {
            EndlessAgony ea = SlenderMain.getInstance().getPerkManager().getEndlessAgony();
            if (ea != null) ea.addToken(slenderMan);
        }

        if (perk instanceof DarkAbyss) {
            DarkAbyss da = SlenderMain.getInstance().getPerkManager().getDarkAbyss();
            if (da != null) {
                if (da.isActive(slenderMan)) {
                    da.deactivate(slenderMan);
                } else {
                    da.activate(slenderMan);
                }
            }
        }

        // Spirit perk: bless allies when survivor dies
        Perk deadPerk = deadPlayer.getPerk(Role.SURVIVOR);
        if (deadPerk instanceof Spirit) {
            java.util.List<Player> allies = new java.util.ArrayList<>();
            arena.getPlayers().entrySet().stream()
                    .filter(e -> e.getValue() == Role.SURVIVOR)
                    .map(java.util.Map.Entry::getKey)
                    .filter(Player::isOnline)
                    .forEach(allies::add);
            if (!allies.isEmpty()) {
                ((Spirit) deadPerk).applyDeathBonus(dead, allies);
            }
        }
    }
}

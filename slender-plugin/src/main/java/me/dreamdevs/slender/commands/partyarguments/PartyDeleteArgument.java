package me.dreamdevs.slender.commands.partyarguments;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Party;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PartyDeleteArgument implements ArgumentCommand {

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Langauge.ADMIN_ONLY_PLAYER.toString());
            return false;
        }

        if (args.length > 1) {
            commandSender.sendMessage(Langauge.ARENA_TOO_MANY_ARGUMENTS.toString());
            return false;
        }

        Player player = (Player) commandSender;
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

        Party party = SlenderMain.getInstance().getPartyManager().getParty(gamePlayer);

        if (party == null) {
            player.sendMessage(Langauge.PARTY_PLAYER_NOT_IN_PARTY.toString());
            return false;
        }

        if (!party.getPartyLeader().equals(gamePlayer.getPlayer())) {
            player.sendMessage(Langauge.PARTY_PLAYER_NOT_LEADER.toString());
            return true;
        }

        new ArrayList<>(party.getMembersMap().keySet()).forEach(member -> {
            member.getOfflinePlayer().getPlayer().sendMessage(Langauge.PARTY_REMOVED_INFO.toString());
        });

        SlenderMain.getInstance().getPartyManager().getParties().remove(party);
        return true;
    }

    @Override
    public String getHelpText() {
        return "&e/party delete - deletes your party";
    }

    @Override
    public String getPermission() {
        return "stopitslender.party";
    }
}
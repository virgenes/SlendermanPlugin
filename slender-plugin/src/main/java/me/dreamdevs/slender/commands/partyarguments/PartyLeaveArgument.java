package me.dreamdevs.slender.commands.partyarguments;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import me.dreamdevs.slender.api.game.party.PartyRole;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Party;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyLeaveArgument implements ArgumentCommand {

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage(Langauge.ADMIN_ONLY_PLAYER.toString());
            return true;
        }

        Player player = (Player) commandSender;
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);

        Party party = SlenderMain.getInstance().getPartyManager().getParty(gamePlayer);
        if(party == null) {
            player.sendMessage(Langauge.PARTY_PLAYER_NOT_IN_PARTY.toString());
            return true;
        }

        PartyRole role = party.getMembersMap().get(gamePlayer);
        if (role == PartyRole.LEADER) {
            party.getMembersMap().keySet().forEach(member -> {
                Player p = member.getOfflinePlayer().getPlayer();
                if (p != null) p.sendMessage(Langauge.PARTY_REMOVED_INFO.toString());
            });
            SlenderMain.getInstance().getPartyManager().getParties().remove(party);
        } else {
            SlenderMain.getInstance().getPartyManager().leaveParty(player, party);
        }

        return true;
    }

    @Override
    public String getHelpText() {
        return "&e/party leave - leave the party";
    }

    @Override
    public String getPermission() {
        return "stopitslender.party";
    }
}

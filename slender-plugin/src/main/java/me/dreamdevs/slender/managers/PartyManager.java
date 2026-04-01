package me.dreamdevs.slender.managers;

import lombok.Getter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.game.party.PartyRole;
import me.dreamdevs.slender.api.game.party.PartySettings;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.game.Party;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PartyManager {

    private final List<Party> parties;
    private final Map<GamePlayer, Party> pendingRequests;

    public PartyManager() {
        this.parties = new ArrayList<>();
        this.pendingRequests = new HashMap<>();
    }

    public void joinParty(Player player, Party party) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if(isInParty(gamePlayer)) {
            gamePlayer.getPlayer().sendMessage(Langauge.PARTY_IS_ALREADY_IN_PARTY.toString());
            return;
        }
        party.getMembersMap().put(gamePlayer, PartyRole.MEMBER);
        party.sendMessage(Langauge.PARTY_PLAYER_JOINED_PARTY.toString().replace("%PLAYER%", player.getName()));
    }

    public void leaveParty(Player player, Party party) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if(!isInParty(gamePlayer)) {
            gamePlayer.getPlayer().sendMessage(Langauge.PARTY_PLAYER_NOT_IN_PARTY.toString());
            return;
        }
        party.getMembersMap().remove(gamePlayer);
        party.sendMessage(Langauge.PARTY_PLAYER_LEFT_PARTY.toString().replace("%PLAYER%", player.getName()));
    }

    public void invitePlayer(Player player, Party party) {
        GamePlayer gamePlayer = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        pendingRequests.put(gamePlayer, party);
        TextComponent textComponent = new TextComponent(Langauge.PARTY_REQUEST_MESSAGE.toString());
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        textComponent.setUnderlined(true);
        gamePlayer.getPlayer().spigot().sendMessage(textComponent);
    }

    public boolean isInParty(GamePlayer gamePlayer) {
        return getParty(gamePlayer) != null;
    }

    public Party getParty(GamePlayer gamePlayer) {
        return parties.stream()
                .filter(party -> party.getMembersMap().containsKey(gamePlayer))
                .findAny()
                .orElse(null);
    }

    public Party getRandomParty() {
        return parties.stream()
                .filter(party -> (boolean) party.getPartySetting(PartySettings.OPEN_PARTY))
                .findAny()
                .orElse(null);
    }

}
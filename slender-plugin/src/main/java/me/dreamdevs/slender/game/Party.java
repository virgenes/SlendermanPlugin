package me.dreamdevs.slender.game;

import me.dreamdevs.slender.api.database.IGamePlayer;
import me.dreamdevs.slender.api.game.party.IParty;
import me.dreamdevs.slender.api.game.party.PartyRole;
import me.dreamdevs.slender.api.game.party.PartySettings;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Party implements IParty {

    private final Map<GamePlayer, PartyRole> members;
    private final Map<PartySettings, Object> partySettings;

    public Party() {
        this.members = new HashMap<>();
        this.partySettings = new HashMap<>();
        this.partySettings.put(PartySettings.OPEN_PARTY, false);
        this.partySettings.put(PartySettings.CHAT_PARTY, false);
        this.partySettings.put(PartySettings.MAX_PLAYERS, 4);
    }

    public void sendMessage(String message) {
        members.keySet().forEach(gamePlayer -> gamePlayer.getPlayer().sendMessage(ColourUtil.colorize(message)));
    }

    @Override
    public Player getPartyLeader() {
        return members.entrySet().stream()
                .filter(gamePlayerPartyRoleEntry -> gamePlayerPartyRoleEntry.getValue().equals(PartyRole.LEADER))
                .map(Map.Entry::getKey)
                .map(GamePlayer::getPlayer)
                .findAny()
                .orElse(null);
    }

    @Override
    public List<IGamePlayer> getMembers() {
        return members.entrySet().stream()
                .filter(gamePlayerPartyRoleEntry -> gamePlayerPartyRoleEntry.getValue().equals(PartyRole.MEMBER))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<GamePlayer, PartyRole> getMembersMap() {
        return members;
    }

    @Override
    public void setPartySetting(PartySettings partySettings, Object value) {
        this.partySettings.put(partySettings, value);
    }

    @Override
    public Object getPartySetting(PartySettings partySettings) {
        return this.partySettings.get(partySettings);
    }
}
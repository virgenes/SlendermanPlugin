package me.dreamdevs.slender.commands.arguments;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.command.CommandSender;

public class ArenaCreateArgument implements ArgumentCommand {

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        if(args.length < 2 || args[1] == null || args[1].isEmpty()) {
            commandSender.sendMessage(Langauge.ARENA_NO_ARENA.toString());
            return true;
        }
        Arena arena = new Arena(args[1]);
        arena.setGameTime(240);
        arena.setMinPlayers(2);
        arena.setMaxPlayers(10);
        arena.setSlenderManSpawnLocation(null);
        arena.startGame();
        SlenderMain.getInstance().getGameManager().getArenas().add(arena);
        commandSender.sendMessage(ColourUtil.colorize("&aYou created new arena with ID: "+arena.getId()+"!"));
        return true;
    }

    @Override
    public String getHelpText() {
        return "&c/stopitslender createarena <id> - creates an arena with specific ID";
    }

    @Override
    public String getPermission() {
        return "stopitslender.admin";
    }
}

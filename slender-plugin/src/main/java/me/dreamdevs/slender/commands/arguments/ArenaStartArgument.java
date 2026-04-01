package me.dreamdevs.slender.commands.arguments;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import me.dreamdevs.slender.api.game.ArenaState;
import me.dreamdevs.slender.game.Arena;
import org.bukkit.command.CommandSender;

public class ArenaStartArgument implements ArgumentCommand {

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        if (args.length < 2 || args[1] == null || args[1].isEmpty()) {
            commandSender.sendMessage(Langauge.ARENA_NO_ARENA.toString());
            return true;
        }

        Arena arena = SlenderMain.getInstance().getGameManager().getArena(args[1]);
        if (arena == null) {
            commandSender.sendMessage(Langauge.ARENA_NO_ARENA.toString());
            return true;
        }

        if (arena.getArenaState() == ArenaState.RUNNING) {
            commandSender.sendMessage(Langauge.ARENA_STILL_RUNNING.toString());
            return true;
        }

        if (arena.getPlayers().size() < arena.getMinPlayers()) {
            commandSender.sendMessage(Langauge.ARENA_STOPPED_STARTING.toString());
            return true;
        }

        arena.setArenaState(ArenaState.STARTING);
        arena.sendMessage(Langauge.ARENA_STARTING_INFO.toString());
        arena.setTimer(30);
        return true;
    }

    @Override
    public String getHelpText() {
        return "&c/stopitslender start <id> - force starts the arena countdown";
    }

    @Override
    public String getPermission() {
        return "stopitslender.admin";
    }
}

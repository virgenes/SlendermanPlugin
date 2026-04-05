package me.dreamdevs.slender.commands.arguments;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import me.dreamdevs.slender.game.Arena;
import me.dreamdevs.slender.menus.ArenaSetupMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * ArenaSetupArgument - Argument to setup an arena
 * Usage: /sis setup <id>
 */
public class ArenaSetupArgument implements ArgumentCommand {

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Langauge.ADMIN_ONLY_PLAYER.toString());
            return true;
        }

        Player player = (Player) commandSender;

        if (args.length != 2) {
            player.sendMessage(Component.text("Usage: /sis setup <id>", NamedTextColor.YELLOW));
            return true;
        }

        String arenaId = args[1];
        Arena arena = SlenderMain.getInstance().getGameManager().getArena(arenaId);

        if (arena == null) {
            player.sendMessage(Langauge.ARENA_NO_ARENA.toString().replace("%MAP_ID%", arenaId));
            return true;
        }

        new ArenaSetupMenu(arena).open(player);
        return true;
    }

    @Override
    public String getHelpText() {
        return "&e/sis setup <id> &7- Open the setup menu for an arena.";
    }

    @Override
    public String getPermission() {
        return "slender.admin";
    }
}

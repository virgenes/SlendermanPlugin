package me.dreamdevs.slender.commands.arguments;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import org.bukkit.command.CommandSender;

public class ReloadArgument implements ArgumentCommand {

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        SlenderMain.getInstance().reloadPlugin();
        commandSender.sendMessage(Langauge.ADMIN_RELOAD_SUCCESS.toString());
        return true;
    }

    @Override
    public String getHelpText() {
        return "&e/sis reload &7- Reloads the plugin configuration and language.";
    }

    @Override
    public String getPermission() {
        return "slender.admin.reload";
    }
}

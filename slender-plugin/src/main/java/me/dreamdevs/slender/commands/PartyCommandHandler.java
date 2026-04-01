package me.dreamdevs.slender.commands;

import lombok.Getter;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Langauge;
import me.dreamdevs.slender.api.commands.ArgumentCommand;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.commands.partyarguments.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PartyCommandHandler implements TabExecutor {

    private final @Getter HashMap<String, Class<? extends ArgumentCommand>> arguments;

    public PartyCommandHandler() {
        this.arguments = new HashMap<>();
        registerCommand("create", PartyCreateArgument.class);
        registerCommand("delete", PartyDeleteArgument.class);
        registerCommand("invite", PartyInviteArgument.class);
        registerCommand("kick", PartyKickMemberArgument.class);
        registerCommand("accept", PartyAcceptInviteArgument.class);
        registerCommand("leave", PartyLeaveArgument.class);
    }

    public void register(SlenderMain plugin) {
        Objects.requireNonNull(plugin.getCommand("party")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("party")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        try {
            if(strings.length >= 1) {
                if(arguments.containsKey(strings[0])) {
                    Class<? extends ArgumentCommand> argumentCommand = arguments.get(strings[0]).asSubclass(ArgumentCommand.class);
                    ArgumentCommand argument = argumentCommand.getConstructor().newInstance();
                    if (commandSender.hasPermission(argument.getPermission())) {
                        argument.execute(commandSender, strings);
                    } else {
                        commandSender.sendMessage(Langauge.ARENA_NO_PERMISSION.toString());
                    }
                } else {
                    commandSender.sendMessage(Langauge.ARENA_NO_ARGUMENT.toString());
                }
                return true;
            } else {
                commandSender.sendMessage(ColourUtil.colorize("&aHelp for SlendermanPlugin Party:"));
                for(Class<? extends ArgumentCommand> argumentCommand : arguments.values()) {
                    commandSender.sendMessage(ColourUtil.colorize(argumentCommand.getConstructor().newInstance().getHelpText()));
                }
                return true;
            }
        } catch (Exception e) {

        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        List<String> completions = new ArrayList<>();
        if(strings.length == 1) {
            StringUtil.copyPartialMatches(strings[0], arguments.keySet(), completions);
            Collections.sort(completions);
            return completions;
        } else return Collections.emptyList();
    }

    private void registerCommand(String command, Class<? extends ArgumentCommand> clazz) {
        arguments.put(command, clazz);
    }

}
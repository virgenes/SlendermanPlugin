package me.dreamdevs.slender.commands.economy;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Statistic;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "balance":
            case "bal":
                handleBalance(sender, args);
                break;
            case "balancetop":
            case "baltop":
                handleBalanceTop(sender);
                break;
            case "pay":
                handlePay(sender, args);
                break;
            case "money":
            case "eco":
            case "economy":
                handleMoney(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColourUtil.colorize("&cOnly players can use this command."));
            return;
        }
        Player player = (Player) sender;
        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
        if (gp == null) {
            sender.sendMessage(ColourUtil.colorize("&cError loading player data."));
            return;
        }
        int coins = gp.getStatistic(Statistic.COINS);
        sender.sendMessage(ColourUtil.colorize("&aYour balance: &6" + coins + " coins"));
    }

    private void handleBalanceTop(CommandSender sender) {
        List<GamePlayer> players = new ArrayList<>(SlenderMain.getInstance().getPlayerManager().getPlayers());
        players.sort((a, b) -> Integer.compare(b.getStatistic(Statistic.COINS), a.getStatistic(Statistic.COINS)));

        sender.sendMessage(ColourUtil.colorize("&6&l=== Balance Top 10 ==="));
        int limit = Math.min(10, players.size());
        for (int i = 0; i < limit; i++) {
            GamePlayer gp = players.get(i);
            String name = gp.getOfflinePlayer().getName() != null ? gp.getOfflinePlayer().getName() : "Unknown";
            sender.sendMessage(ColourUtil.colorize("&e#" + (i + 1) + " &f" + name + " &6- " + gp.getStatistic(Statistic.COINS) + " coins"));
        }
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColourUtil.colorize("&cOnly players can use this command."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColourUtil.colorize("&cUsage: /sis pay <player> <amount>"));
            return;
        }

        Player payer = (Player) sender;
        GamePlayer payerGP = SlenderMain.getInstance().getPlayerManager().getPlayer(payer);
        if (payerGP == null) return;

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ColourUtil.colorize("&cPlayer not found or offline."));
            return;
        }

        GamePlayer targetGP = SlenderMain.getInstance().getPlayerManager().getPlayer(target);
        if (targetGP == null) return;

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColourUtil.colorize("&cInvalid amount."));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(ColourUtil.colorize("&cAmount must be positive."));
            return;
        }

        int payerCoins = payerGP.getStatistic(Statistic.COINS);
        if (payerCoins < amount) {
            sender.sendMessage(ColourUtil.colorize("&cYou don't have enough coins!"));
            return;
        }

        payerGP.setStatistic(Statistic.COINS, payerCoins - amount);
        targetGP.setStatistic(Statistic.COINS, targetGP.getStatistic(Statistic.COINS) + amount);

        sender.sendMessage(ColourUtil.colorize("&aYou sent &6" + amount + " coins &ato &e" + target.getName()));
        target.sendMessage(ColourUtil.colorize("&e" + payer.getName() + " &asent you &6" + amount + " coins&a!"));
    }

    private void handleMoney(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMoneyHelp(sender);
            return;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "help":
                sendMoneyHelp(sender);
                break;
            case "give":
                handleMoneyGive(sender, args);
                break;
            case "take":
            case "remove":
                handleMoneyTake(sender, args);
                break;
            case "set":
                handleMoneySet(sender, args);
                break;
            case "reload":
            case "rl":
                handleMoneyReload(sender);
                break;
            default:
                sendMoneyHelp(sender);
                break;
        }
    }

    private void handleMoneyGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("slender.economy.give")) {
            sender.sendMessage(ColourUtil.colorize("&cNo permission."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ColourUtil.colorize("&cUsage: /sis money give <player> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ColourUtil.colorize("&cPlayer not found or offline."));
            return;
        }

        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(target);
        if (gp == null) return;

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColourUtil.colorize("&cInvalid amount."));
            return;
        }

        gp.setStatistic(Statistic.COINS, gp.getStatistic(Statistic.COINS) + amount);
        sender.sendMessage(ColourUtil.colorize("&aGave &6" + amount + " coins &ato &e" + target.getName()));
        target.sendMessage(ColourUtil.colorize("&aYou received &6" + amount + " coins &afrom an admin!"));
    }

    private void handleMoneyTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("slender.economy.take")) {
            sender.sendMessage(ColourUtil.colorize("&cNo permission."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ColourUtil.colorize("&cUsage: /sis money take <player> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ColourUtil.colorize("&cPlayer not found or offline."));
            return;
        }

        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(target);
        if (gp == null) return;

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColourUtil.colorize("&cInvalid amount."));
            return;
        }

        int current = gp.getStatistic(Statistic.COINS);
        gp.setStatistic(Statistic.COINS, Math.max(0, current - amount));
        sender.sendMessage(ColourUtil.colorize("&aTook &6" + amount + " coins &afrom &e" + target.getName()));
        target.sendMessage(ColourUtil.colorize("&cAn admin took &6" + amount + " coins &cfrom you!"));
    }

    private void handleMoneySet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("slender.economy.set")) {
            sender.sendMessage(ColourUtil.colorize("&cNo permission."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ColourUtil.colorize("&cUsage: /sis money set <player> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ColourUtil.colorize("&cPlayer not found or offline."));
            return;
        }

        GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(target);
        if (gp == null) return;

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColourUtil.colorize("&cInvalid amount."));
            return;
        }

        gp.setStatistic(Statistic.COINS, amount);
        sender.sendMessage(ColourUtil.colorize("&aSet &e" + target.getName() + "&a's balance to &6" + amount + " coins"));
        target.sendMessage(ColourUtil.colorize("&aAn admin set your balance to &6" + amount + " coins&a!"));
    }

    private void handleMoneyReload(CommandSender sender) {
        if (!sender.hasPermission("slender.economy.reload")) {
            sender.sendMessage(ColourUtil.colorize("&cNo permission."));
            return;
        }
        SlenderMain.getInstance().getDatabase().saveData();
        sender.sendMessage(ColourUtil.colorize("&aEconomy data saved!"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColourUtil.colorize("&6&l=== SlendermanPlugin Economy ==="));
        sender.sendMessage(ColourUtil.colorize("&e/sis balance &7- Check your balance"));
        sender.sendMessage(ColourUtil.colorize("&e/sis balancetop &7- Top 10 richest players"));
        sender.sendMessage(ColourUtil.colorize("&e/sis pay <player> <amount> &7- Send coins"));
        sender.sendMessage(ColourUtil.colorize("&e/sis money &7- Admin economy commands"));
    }

    private void sendMoneyHelp(CommandSender sender) {
        sender.sendMessage(ColourUtil.colorize("&6&l=== Economy Admin ==="));
        sender.sendMessage(ColourUtil.colorize("&e/sis money give <player> <amount> &7- Give coins"));
        sender.sendMessage(ColourUtil.colorize("&e/sis money take <player> <amount> &7- Take coins"));
        sender.sendMessage(ColourUtil.colorize("&e/sis money set <player> <amount> &7- Set balance"));
        sender.sendMessage(ColourUtil.colorize("&e/sis money reload &7- Save data"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("balance", "bal", "balancetop", "baltop", "pay", "money");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pay")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("money")) {
            List<String> subs = Arrays.asList("help", "give", "take", "set", "reload");
            for (String s : subs) {
                if (s.startsWith(args[1].toLowerCase())) completions.add(s);
            }
        } else if ((args.length == 3 && args[0].equalsIgnoreCase("pay")) ||
                   (args.length == 3 && args[0].equalsIgnoreCase("money"))) {
            if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("take") ||
                args[1].equalsIgnoreCase("set")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}

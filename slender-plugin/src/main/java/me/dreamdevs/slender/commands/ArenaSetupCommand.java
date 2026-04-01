package me.dreamdevs.slender.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.dreamdevs.slender.SlenderMain;

/**
 * ArenaSetupCommand - Comando para configuración de arenas
 * Comando: /sis setup <id>
 * Permite configurar arenas con una interfaz mejorada
 */
public class ArenaSetupCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("slender.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }
        
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /sis setup <id>");
            player.sendMessage(ChatColor.GRAY + "Ejemplo: /sis setup arena1");
            return true;
        }
        
        String arenaId = args[0];
        
        // Mensaje de configuración
        player.sendMessage(ChatColor.GREEN + "=== Configuración de Arena: " + ChatColor.YELLOW + arenaId + ChatColor.GREEN + " ===");
        player.sendMessage(ChatColor.AQUA + "Usa los siguientes comandos para configurar:");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " setspawn - Establecer spawn principal");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " setslender - Establecer spawn de SlenderMan");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " addpage - Añadir ubicación de página");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " setlobby - Establecer lobby");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " setbounds - Establecer límites");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " save - Guardar arena");
        player.sendMessage(ChatColor.WHITE + "  • /sis " + arenaId + " delete - Eliminar arena");
        player.sendMessage(ChatColor.GREEN + "Usa /sis " + arenaId + " info para ver información actual");
        
        return true;
    }
}

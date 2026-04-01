package me.dreamdevs.slender.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * ArenaSetupCommand - Comando para configuración de arenas
 * Comando: /sis setup <id>
 * Permite configurar arenas con una interfaz mejorada
 */
public class ArenaSetupCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("slender.admin")) {
            player.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }
        
        if (args.length != 1) {
            player.sendMessage(Component.text("Uso: /sis setup <id>", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Ejemplo: /sis setup arena1", NamedTextColor.GRAY));
            return true;
        }
        
        String arenaId = args[0];
        
        // Mensaje de configuración
        player.sendMessage(Component.text()
                .append(Component.text("=== Configuración de Arena: ", NamedTextColor.GREEN))
                .append(Component.text(arenaId, NamedTextColor.YELLOW))
                .append(Component.text(" ===", NamedTextColor.GREEN))
                .build());
        player.sendMessage(Component.text("Usa los siguientes comandos para configurar:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  • /sis " + arenaId + " setspawn - Establecer spawn principal", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  • /sis " + arenaId + " setslender - Establecer spawn de SlenderMan", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  • /sis " + arenaId + " addpage - Añadir ubicación de página", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  • /sis " + arenaId + " setlobby - Establecer lobby", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  • /sis " + arenaId + " setbounds - Establecer límites", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  • /sis " + arenaId + " save - Guardar arena", NamedTextColor.WHITE));
        player.sendMessage(Component.text("  • /sis " + arenaId + " delete - Eliminar arena", NamedTextColor.WHITE));
        player.sendMessage(Component.text("Usa /sis " + arenaId + " info para ver información actual", NamedTextColor.GREEN));
        
        return true;
    }
}

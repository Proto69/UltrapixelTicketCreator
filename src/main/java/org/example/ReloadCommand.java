package org.example;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;


public class ReloadCommand implements CommandExecutor {
    private final TicketCreator plugin;

    public ReloadCommand(TicketCreator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Checking the sender for required permission
        if (sender.hasPermission("tickets.reload")) {

            // Getting the time before the reload
            long before = System.currentTimeMillis();

            // Reloading the plugin
            plugin.reload();

            // Getting the time after the reload
            long after = System.currentTimeMillis();

            long time = after - before;

            Map<String, String> map = new HashMap<>();
            map.put("time", String.valueOf(time));

            UsefulMethods.sendMessage(sender, map, "reloaded");
        }
        return true;
    }
}
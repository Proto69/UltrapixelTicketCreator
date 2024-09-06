package org.example;

import com.olziedev.playerwarps.api.PlayerWarpsAPI;
import com.olziedev.playerwarps.api.warp.Warp;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.*;

public class BuyTicket implements CommandExecutor {

    private final TicketCreator plugin;

//    String: tier;amount;price

    public BuyTicket(TicketCreator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (Objects.equals(args[1], "confirm")){
            String order = this.plugin.orders.get(player);
            if (order == null){
                String message = UsefulMethods.getMessage("no-orders");
                player.sendMessage(ColorUtil.translateHexColorCodes(message));
                return true;
            }

            giveItem(order, player);
            return true;

        } else if (Objects.equals(args[1], "cancel")){
            if (this.plugin.orders.containsKey(player)){
                this.plugin.orders.remove(player);

                String message = UsefulMethods.getMessage("cancelled-order");
                player.sendMessage(ColorUtil.translateHexColorCodes(message));
                return true;
            }

            String message = UsefulMethods.getMessage("no-orders");
            player.sendMessage(ColorUtil.translateHexColorCodes(message));
            return true;
        }

        int quantity;

        if (args.length < 3) {
            String message = UsefulMethods.getMessage("usage");
            player.sendMessage(ColorUtil.translateHexColorCodes(message));
            return true;
        } else if (args.length == 4) {
            quantity = Integer.parseInt(args[3]);
        } else {
            quantity = 1;
        }

        int tier = switch (args[1]) {
            default -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
        };

        if (!(player.hasPermission("tickets.tier.5") || (player.hasPermission("tickets.tier.4") && tier <= 4) || (player.hasPermission("tickets.tier.3") && tier <= 3) || (player.hasPermission("tickets.tier.2") && tier <= 2) || (player.hasPermission("tickets.tier.1") && tier == 1))){
            Map<String, String> map = new HashMap<>();
            map.put("tier", args[1]);

            UsefulMethods.sendMessage(player, map, "no-tier-permission");
            return true;
        }

        String warpName = args[2];

        double playerBalance = Math.round(TicketCreator.econ.getBalance(player));

        PlayerWarpsAPI.getInstance(api -> {
            boolean isOwnerOfWarp;

            Map<String, String> map = new HashMap<>();
            map.put("warpName", warpName);
            map.put("quantity", Integer.toString(quantity));
            map.put("balance", Double.toString(playerBalance));

            String tierIcon = switch (tier) {
                default -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
            };

            map.put("tier", tierIcon);

            double price = Double.parseDouble(UsefulMethods.readConfig("tiers." + tier + ".price")) * quantity;

            map.put("price", Double.toString(price));

            if (Objects.equals(UsefulMethods.readConfig("owner-check"), "true")){
                Warp warp = api.getPlayerWarp(warpName, sender);
                if (warp == null){
                    UsefulMethods.sendMessage(player, map, "warp-not-found");
                    return;
                }
                UUID ownerUUID = warp.getUUID();

                UUID uuid = player.getUniqueId();

                isOwnerOfWarp = Objects.equals(uuid.toString(), ownerUUID.toString());

                if (!isOwnerOfWarp){
                    UsefulMethods.sendMessage(player, map, "not-owner");
                    return;
                }
            }

            if (playerBalance < price){
                UsefulMethods.sendMessage(player, map, "insufficient-balance");
                return;
            }
            String code = tier + ";" + quantity + ";" + price + ";" + warpName;

            if (this.plugin.orders.containsKey(player)){
                UsefulMethods.sendMessage(player, map, "unconfirmed-order");
                return;
            }

            this.plugin.orders.put(player, code);

            TextComponent confirmButton = new TextComponent(ColorUtil.translateHexColorCodes(UsefulMethods.getMessage("confirm-order")));
            TextComponent cancelButton = new TextComponent(ColorUtil.translateHexColorCodes(UsefulMethods.getMessage("cancel-order")));

            // Set the click event to run a command when clicked
            confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tickets buy confirm"));
            cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tickets buy cancel"));

            confirmButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(ColorUtil.translateHexColorCodes(UsefulMethods.readConfig("messages.confirm-order-hover"))).create()));
            cancelButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(ColorUtil.translateHexColorCodes(UsefulMethods.readConfig("messages.cancel-order-hover"))).create()));

            UsefulMethods.sendMessage(player, map, "placed-order");
            player.spigot().sendMessage(confirmButton);
            player.spigot().sendMessage(cancelButton);
        });
        return true;
    }

    private void giveItem(String code, Player player){
        String[] args = code.split(";");
        int tier = Integer.parseInt(args[0]);
        int quantity = Integer.parseInt(args[1]);
        double price = Double.parseDouble(args[2]);
        String warpName = args[3];

        Map<String, String> map = new HashMap<>();
        String tierIcon = switch (tier) {
            default -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
        };

        map.put("tier", tierIcon);
        map.put("quantity", String.valueOf(quantity));
        map.put("price", String.valueOf(price));
        map.put("balance", String.valueOf(Math.round(TicketCreator.econ.getBalance(player))));
        map.put("warpName", warpName);


        // Retrieve the item name and lore from the config
        String itemName = UsefulMethods.readConfig("tiers." + tier + ".name");
        List<String> itemLore = plugin.getConfig().getStringList("tiers." + tier + ".lore");

        itemName = UsefulMethods.replacePlaceholders(itemName, map);
        List<String> finalLore = new ArrayList<>();
        for (String line : itemLore) {
            finalLore.add(ColorUtil.translateHexColorCodes(UsefulMethods.replacePlaceholders(line, map)));
        }

        // Create the item
        ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(UsefulMethods.readConfig("tiers." + tier + ".type")))); // Change the material as needed
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translateHexColorCodes(itemName));
            meta.setLore(finalLore);

            if (Objects.equals(UsefulMethods.readConfig("tiers." + tier + ".glowing"), "true")){
                meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        item.setAmount(quantity);

        if (UsefulMethods.hasEnoughSpace(player, item)){
            TicketCreator.econ.withdrawPlayer(player, price);

            // Give the item to the player
            player.getInventory().addItem(item);

            UsefulMethods.sendMessage(player, map, "successful-purchase");

            this.plugin.orders.remove(player);
        } else {
            UsefulMethods.sendMessage(player, map, "not-enough-space");
        }
    }
}

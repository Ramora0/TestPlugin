package org.leedavis.testplugin;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.leedavis.testplugin.database.CropTraits;
import org.leedavis.testplugin.utils.CropNBT;

public final class TestPlugin extends JavaPlugin {

    private CropTraits cropTraits;
    private CropEventHandler cropEventHandler;

    @Override
    public void onEnable() {
        System.out.println("Boo!");

        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            cropTraits = new CropTraits(getDataFolder().getAbsolutePath() + "/croptraits.db");
            cropEventHandler = new CropEventHandler(cropTraits, this);

            getServer().getPluginManager().registerEvents(cropEventHandler, this);
        } catch (SQLException ex) {
            ex.printStackTrace();

            System.out.println("Failed to connect to crop traits database.");

            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        System.out.println("Bye :(");

        try {
            cropTraits.closeConnection();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("croptraits")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 3) {
                // Command format: /croptraits <x> <y> <z>
                try {
                    int x = Integer.parseInt(args[0]);
                    int y = Integer.parseInt(args[1]);
                    int z = Integer.parseInt(args[2]);

                    // First check if crop data exists at this location
                    if (cropTraits.hasCrop(player.getWorld().getName(), x, y, z)) {
                        CropNBT cropData = cropTraits.getCropTraits(
                                player.getWorld().getName(), x, y, z);

                        if (cropData != null) {
                            player.sendMessage("Crop traits at " + x + "," + y + "," + z + ":");
                            player.sendMessage(cropData.toString());
                        } else {
                            player.sendMessage("Error retrieving crop data at location " + x + "," + y + "," + z);
                        }
                    } else {
                        player.sendMessage("No crop traits found at location " + x + "," + y + "," + z);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid coordinates! Use: /croptraits <x> <y> <z>");
                }
            } else {
                player.sendMessage("Usage: /croptraits [<x> <y> <z>]");
                player.sendMessage("  - With coordinates: Check crop at specified location");
            }

            return true;
        }

        if (command.getName().equalsIgnoreCase("environment")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;

            // Get player's current location
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();

            double temperature = player.getWorld().getBlockAt(x, y, z).getTemperature();
            double humidity = player.getWorld().getBlockAt(x, y, z).getHumidity();

            player.sendMessage("Environmental data at your location (" + x + "," + y + "," + z + "):");
            player.sendMessage("Temperature: " + String.format("%.2f", temperature));
            player.sendMessage("Humidity: " + String.format("%.2f", humidity));
            player.sendMessage("Biome: " + player.getLocation().getBlock().getBiome().toString());

            return true;
        }

        return false;
    }

}

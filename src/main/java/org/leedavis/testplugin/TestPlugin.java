package org.leedavis.testplugin;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.leedavis.testplugin.database.CropData;
import org.leedavis.testplugin.database.CropTraits;

public final class TestPlugin extends JavaPlugin implements Listener {

    private CropTraits cropTraits;

    @Override
    public void onEnable() {
        System.out.println("Boo!");

        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            cropTraits = new CropTraits(getDataFolder().getAbsolutePath() + "/croptraits.db");
        } catch (SQLException ex) {
            ex.printStackTrace();

            System.out.println("Failed to connect to crop traits database.");

            Bukkit.getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(this, this);
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

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        // Check if the recipe result is bread
        if (event.getRecipe() == null || event.getRecipe().getResult().getType() != Material.BREAD) {
            return;
        }

        ItemStack[] matrix = event.getInventory().getMatrix();

        double totalQuality = 0.0;

        for (ItemStack item : matrix) {
            if (item != null && item.getType() == Material.WHEAT) {
                CropData cropData = CropData.fromNBT(item);
                if (cropData != null) {
                    totalQuality += cropData.getQuality() / 3.;
                }
            }
        }

        int averageQuality = (int) Math.round(totalQuality);

        // Create bread with average quality
        ItemStack bread = new ItemStack(Material.BREAD);
        ItemMeta breadMeta = bread.getItemMeta();
        breadMeta.getPersistentDataContainer().set(
                new NamespacedKey("testplugin", "quality"),
                PersistentDataType.INTEGER, averageQuality);

        // Optional: Add lore to show quality
        breadMeta.setLore(java.util.Arrays.asList(CropData.getQualityDisplay(averageQuality)));

        bread.setItemMeta(breadMeta);
        event.getInventory().setResult(bread);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.WHEAT) {
            try {
                ItemStack placedItem = event.getItemInHand();
                CropData cropData = CropData.fromNBT(placedItem);

                cropTraits.createCrop(
                        block.getWorld().getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ(),
                        cropData);
            } catch (SQLException ex) {
                ex.printStackTrace();
                System.out.println("Failed to create crop in database for location: " +
                        block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Breaking block under?
        long startTime = System.nanoTime();
        Block block = event.getBlock();

        if (block.getType() == Material.WHEAT) {
            try {
                // Get crop traits before destroying it
                long dbReadStart = System.nanoTime();
                CropData cropData = cropTraits.getCropTraits(
                        block.getWorld().getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ());
                long dbReadEnd = System.nanoTime();
                double dbReadMs = (dbReadEnd - dbReadStart) / 1_000_000.0;
                System.out.println("  DB read took: " + String.format("%.3f", dbReadMs) + " ms");

                if (cropData == null) {
                    System.out.println("Uhhhh no crop data for location: " +
                            block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ());
                    return;
                }

                long dbDeleteStart = System.nanoTime();
                cropTraits.deleteCrop(
                        block.getWorld().getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ());
                long dbDeleteEnd = System.nanoTime();
                double dbDeleteMs = (dbDeleteEnd - dbDeleteStart) / 1_000_000.0;
                System.out.println("  DB delete took: " + String.format("%.3f", dbDeleteMs) + " ms");

                Ageable age = (Ageable) block.getBlockData();

                ItemStack drop = age.getAge() == age.getMaximumAge() ? cropData.getCrop() : cropData.getSeeds();

                long dropStart = System.nanoTime();
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
                long dropEnd = System.nanoTime();
                double dropMs = (dropEnd - dropStart) / 1_000_000.0;
                System.out.println("  Block removal & item drop took: " + String.format("%.3f", dropMs) + " ms");
            } catch (SQLException ex) {
                ex.printStackTrace();
                System.out.println("Failed to handle crop destruction for location: " +
                        block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ());
            }
        }

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("BlockBreak event took: " + String.format("%.3f", durationMs) + " ms");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the player right-clicked (either air or block)
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();

        // Check if the player is holding wheat
        if (itemInHand == null || itemInHand.getType() != Material.WHEAT) {
            return;
        }

        // Get the NBT data from the wheat
        CropData cropData = CropData.fromNBT(itemInHand);

        // If no NBT data, use default values (quality: 0, resistance: 0, yield: 0)
        if (cropData == null) {
            cropData = new CropData(0, 0, 0);
        }

        // Generate random number of seeds (1-3)
        int seedCount = (int) (Math.random() * 3) + 1;

        // Create seeds with the same NBT data
        cropData.mutate();
        ItemStack seeds = cropData.getSeeds();
        seeds.setAmount(seedCount);

        // Add seeds to player's inventory
        player.getInventory().addItem(seeds);

        // Remove one wheat from the player's inventory
        itemInHand.setAmount(itemInHand.getAmount() - 1);

        // Send feedback message to player
        player.sendMessage("You extracted " + seedCount + " seeds from the wheat!");

        // Prevent the default interaction behavior
        event.setCancelled(true);
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

                    CropData cropData = cropTraits.getCropTraits(
                            player.getWorld().getName(), x, y, z);

                    if (cropData != null) {
                        player.sendMessage("Crop traits at " + x + "," + y + "," + z + ":");
                        player.sendMessage(cropData.toString());
                    } else {
                        player.sendMessage("No crop found at location " + x + "," + y + "," + z);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid coordinates! Use: /croptraits <x> <y> <z>");
                } catch (SQLException e) {
                    player.sendMessage("Database error occurred while retrieving crop traits.");
                    e.printStackTrace();
                }
            } else {
                player.sendMessage("Usage: /croptraits [<x> <y> <z>]");
                player.sendMessage("  - With coordinates: Check crop at specified location");
            }

            return true;
        }

        return false;
    }
}

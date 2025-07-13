package org.leedavis.testplugin;

import java.sql.SQLException;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.leedavis.testplugin.database.CropTraits;
import org.leedavis.testplugin.utils.AttributeManager;
import org.leedavis.testplugin.utils.CropNBT;
import org.leedavis.testplugin.utils.QualityNBT;

import net.md_5.bungee.api.ChatColor;

public class CropEventHandler implements Listener {

  private final CropTraits cropTraits;
  private final TestPlugin plugin;

  public CropEventHandler(CropTraits cropTraits, TestPlugin plugin) {
    this.cropTraits = cropTraits;
    this.plugin = plugin;
  }

  @EventHandler
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    // Only handle player pickups
    if (!(event.getEntity() instanceof Player)) {
      return;
    }

    ItemStack item = event.getItem().getItemStack();

    if ((item.getType() == Material.WHEAT || item.getType() == Material.WHEAT_SEEDS)
        && !AttributeManager.hasData(item, CropNBT.class)) {

      CropNBT defaultData = new CropNBT(0, 0, 0);
      ItemStack updatedItem = item.getType() == Material.WHEAT ? defaultData.getCrop() : defaultData.getSeeds();
      updatedItem.setAmount(item.getAmount());

      event.getItem().setItemStack(updatedItem);
    }
  }

  @EventHandler
  public void onPrepareItemCraft(PrepareItemCraftEvent event) {
    if (event.getRecipe() == null || event.getRecipe().getResult().getType() != Material.BREAD) {
      return;
    }

    ItemStack[] matrix = event.getInventory().getMatrix();

    double totalQuality = 0.0;

    for (ItemStack item : matrix) {
      if (item != null && item.getType() == Material.WHEAT) {
        CropNBT cropData = AttributeManager.getData(item, CropNBT.class);
        if (cropData != null) {
          totalQuality += cropData.getQuality() / 3.;
        }
      }
    }

    int averageQuality = (int) Math.round(totalQuality);

    // Create bread with average quality using AttributeManager
    QualityNBT breadData = new QualityNBT(averageQuality);
    ItemStack bread = breadData.imbueToItem(new ItemStack(Material.BREAD));
    event.getInventory().setResult(bread);
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();

    if (block.getType() == Material.WHEAT) {
      try {
        ItemStack placedItem = event.getItemInHand();
        CropNBT cropData = AttributeManager.getData(placedItem, CropNBT.class);

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
      // Get crop traits before destroying it
      long dbReadStart = System.nanoTime();
      CropNBT cropData = cropTraits.getCropTraits(
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

      try {
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

        // TODO: Broken with a hoe increases stats

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
    // TODO: Right clicking harvesting with hoe keeps some growth stages
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

    // Handle wheat right-click (extract seeds)
    // Get the NBT data from the wheat
    CropNBT cropData = AttributeManager.getData(itemInHand, CropNBT.class);

    // If no NBT data, use default values (quality: 0, resistance: 0, yield: 0)
    if (cropData == null) {
      cropData = new CropNBT(0, 0, 0);
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

    // Prevent the default interaction behavior
    event.setCancelled(true);
  }

  @EventHandler
  public void onBlockGrow(BlockGrowEvent event) {
    Block block = event.getBlock();

    if (block.getType() == Material.WHEAT) {
      CropNBT data = cropTraits.getCropTraits(block.getWorld().getName(),
          block.getX(),
          block.getY(),
          block.getZ());

      double targetHumidity = this.plugin.getConfig().getDouble("crops.wheat.optimal_humidity");
      double targetTemperature = this.plugin.getConfig().getDouble("crops.wheat.optimal_temperature");

      double cancelChance = data.getCancelChance(block, targetHumidity, targetTemperature);

      if (Math.random() < cancelChance) {
        System.out.println("Blocked growth for wheat at " +
            block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," +
            block.getZ());
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onFoodLevelChange(FoodLevelChangeEvent event) {
    // Only handle player food level changes
    if (!(event.getEntity() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getEntity();
    ItemStack consumedItem = event.getItem();

    // Check if the consumed item is bread
    if (consumedItem == null || consumedItem.getType() != Material.BREAD) {
      return;
    }

    // Get quality from bread NBT data using AttributeManager
    QualityNBT qualityData = AttributeManager.getData(consumedItem, QualityNBT.class);
    int quality = qualityData != null ? qualityData.getQuality() : 0;

    // Send feedback message to player
    player.sendMessage(
        "You ate some " + QualityNBT.getQualityDisplay(quality) + ChatColor.WHITE + " bread!");
  }
}

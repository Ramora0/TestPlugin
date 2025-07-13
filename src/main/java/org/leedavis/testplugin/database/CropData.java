package org.leedavis.testplugin.database;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class CropData {
  private static final double NO_CHANGE_PROBABILITY = 0.; // Desired probability for no change

  // ========================================
  // FIELDS
  // ========================================

  private int quality;
  private int resistance;
  private int yield;

  // ========================================
  // CONSTRUCTOR
  // ========================================

  public CropData(int quality, int resistance, int yield) {
    this.quality = quality;
    this.resistance = resistance;
    this.yield = yield;
  }

  // ========================================
  // MUTATION METHODS
  // ========================================

  public void mutate() {
    if (Math.random() < NO_CHANGE_PROBABILITY) {
      return; // No change
    }

    // Randomly select a field to decrease (> -5)
    int decreaseField;
    do {
      decreaseField = (int) (Math.random() * 3); // 0=quality, 1=resistance, 2=yield
    } while ((decreaseField == 0 && quality <= -5) ||
        (decreaseField == 1 && resistance <= -5) ||
        (decreaseField == 2 && yield <= -5));

    // Randomly select a field to increase (< 5)
    int increaseField;
    do {
      increaseField = (int) (Math.random() * 3); // 0=quality, 1=resistance, 2=yield
    } while ((increaseField == 0 && quality >= 5) ||
        (increaseField == 1 && resistance >= 5) ||
        (increaseField == 2 && yield >= 5) ||
        increaseField == decreaseField);

    // Apply mutations
    switch (decreaseField) {
      case 0:
        quality--;
        break;
      case 1:
        resistance--;
        break;
      case 2:
        yield--;
        break;
    }

    switch (increaseField) {
      case 0:
        quality++;
        break;
      case 1:
        resistance++;
        break;
      case 2:
        yield++;
        break;
    }
  }

  public double getCancelChance(Block block, double targetHumidity, double targetTemperature) {
    double humidity = block.getHumidity() - targetHumidity;
    double temperature = block.getTemperature() - targetTemperature;

    double rFactor = Math.pow(resistance - 5, 2) / 5.;
    double hFactor = Math.exp(-Math.pow(humidity, 2) * rFactor);
    double tFactor = Math.exp(-Math.pow(temperature, 2) * rFactor);

    return 1 - hFactor * tFactor;
  }

  // ========================================
  // ITEM STACK GENERATION METHODS
  // ========================================

  public ItemStack getCrop() {
    double adjYield = Math.pow(2.5, yield / 5.0 + 1) / 2;
    int amount = 0;
    while (adjYield > 0) {
      if (adjYield > 1) {
        amount++;
      } else if (Math.random() < adjYield) {
        amount++;
      }
      adjYield -= 1.0;
    }

    return imbueNBT(new ItemStack(Material.WHEAT, amount));
  }

  public ItemStack getSeeds() {
    return imbueNBT(new ItemStack(Material.WHEAT_SEEDS, 1));
  }

  private ItemStack imbueNBT(ItemStack item) {
    ItemMeta meta = item.getItemMeta();

    meta.setLore(Arrays.asList(
        getQualityDisplay(quality),
        "Resistance: " + resistance,
        "Yield: " + yield));

    PersistentDataContainer nbt = meta.getPersistentDataContainer();
    nbt.set(new NamespacedKey("testplugin", "quality"), PersistentDataType.INTEGER, quality);
    nbt.set(new NamespacedKey("testplugin", "resistance"), PersistentDataType.INTEGER, resistance);
    nbt.set(new NamespacedKey("testplugin", "yield"), PersistentDataType.INTEGER, yield);

    item.setItemMeta(meta);
    return item;
  }

  // ========================================
  // DISPLAY METHODS
  // ========================================

  public static String getQualityDisplay(int quality) {
    if (quality <= -8) {
      return ChatColor.DARK_RED + "Terrible (" + quality + ")"; // Dark Red
    } else if (quality <= -5) {
      return ChatColor.RED + "Poor (" + quality + ")"; // Red
    } else if (quality <= -2) {
      return ChatColor.YELLOW + "Below Average (" + quality + ")"; // Yellow
    } else if (quality <= 2) {
      return ChatColor.WHITE + "Average (" + quality + ")"; // White
    } else if (quality <= 4) {
      return ChatColor.GREEN + "Good (" + quality + ")"; // Green
    } else if (quality <= 7) {
      return ChatColor.GOLD + "Excellent (" + quality + ")"; // Gold
    } else {
      return ChatColor.LIGHT_PURPLE + "Perfect (" + quality + ")"; // Light Purple
    }
  }

  // ========================================
  // SERIALIZATION METHODS
  // ========================================

  public static CropData fromNBT(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null)
      return null;

    PersistentDataContainer nbt = meta.getPersistentDataContainer();
    int quality = nbt.getOrDefault(new NamespacedKey("testplugin", "quality"), PersistentDataType.INTEGER, 0);
    int resistance = nbt.getOrDefault(new NamespacedKey("testplugin", "resistance"), PersistentDataType.INTEGER,
        0);
    int yield = nbt.getOrDefault(new NamespacedKey("testplugin", "yield"), PersistentDataType.INTEGER, 0);

    return new CropData(quality, resistance, yield);
  }

  public static boolean hasNBT(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null)
      return false;

    PersistentDataContainer nbt = meta.getPersistentDataContainer();
    return nbt.has(new NamespacedKey("testplugin", "quality"), PersistentDataType.INTEGER) &&
        nbt.has(new NamespacedKey("testplugin", "resistance"), PersistentDataType.INTEGER) &&
        nbt.has(new NamespacedKey("testplugin", "yield"), PersistentDataType.INTEGER);
  }

  // ========================================
  // GETTER METHODS
  // ========================================

  public int getQuality() {
    return quality;
  }

  public int getResistance() {
    return resistance;
  }

  public int getYield() {
    return yield;
  }

  // ========================================
  // UTILITY METHODS
  // ========================================

  @Override
  public String toString() {
    return String.format("Quality: %d, Resistance: %d, Yield: %d", quality, resistance, yield);
  }
}

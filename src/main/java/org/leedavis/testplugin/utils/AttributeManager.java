package org.leedavis.testplugin.utils;

import org.bukkit.inventory.ItemStack;

public class AttributeManager {

  public static <T extends AttributeData> boolean hasData(ItemStack item, Class<T> dataType) {
    try {
      T instance = dataType.getDeclaredConstructor().newInstance();
      return instance.hasData(item);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static <T extends AttributeData> T getData(ItemStack item, Class<T> dataType) {
    try {
      T instance = dataType.getDeclaredConstructor().newInstance();
      if (instance.hasData(item)) {
        instance.loadFromItem(item);
        return instance;
      }
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static <T extends AttributeData> ItemStack imbueData(ItemStack item, T data) {
    return data.imbueToItem(item);
  }
}

package org.leedavis.testplugin.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class AttributeData {

    private List<Field> getAllFields() {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = this.getClass();

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                fields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    public boolean hasData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        for (Field field : getAllFields()) {
            if (field.isAnnotationPresent(Attribute.class)) {
                if (!nbt.has(new NamespacedKey("testplugin", field.getName()), PersistentDataType.INTEGER)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void loadFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        for (Field field : getAllFields()) {
            if (field.isAnnotationPresent(Attribute.class)) {
                NamespacedKey key = new NamespacedKey("testplugin", field.getName());

                if (nbt.has(key, PersistentDataType.INTEGER)) {
                    try {
                        field.setAccessible(true);
                        field.set(this, nbt.get(key, PersistentDataType.INTEGER));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Override this method in subclasses to provide additional lore lines
     * that will be displayed at the beginning of the item's lore.
     * 
     * @return a list of additional lore lines to display, or empty list if none
     */
    protected List<String> getAdditionalLore() {
        return new ArrayList<>();
    }

    public ItemStack imbueToItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        List<String> lore = new ArrayList<>();

        // Add any additional lore from subclasses first
        lore.addAll(getAdditionalLore());

        for (Field field : getAllFields()) {
            if (field.isAnnotationPresent(Attribute.class)) {
                Attribute attr = field.getAnnotation(Attribute.class);
                NamespacedKey key = new NamespacedKey("testplugin", field.getName());

                try {
                    field.setAccessible(true);
                    int value = (int) field.get(this);
                    nbt.set(key, PersistentDataType.INTEGER, value);

                    if (attr.includeInLore()) {
                        lore.add(field.getName() + ": " + value);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a copy of this AttributeData instance with all attribute values
     * copied.
     * 
     * @return a new instance of the same type with copied attribute values
     * @throws RuntimeException if the copy operation fails
     */
    public <T extends AttributeData> T copy() {
        try {
            // Create a new instance of the same type
            @SuppressWarnings("unchecked")
            T copy = (T) this.getClass().getDeclaredConstructor().newInstance();

            // Copy all attribute values
            for (Field field : getAllFields()) {
                if (field.isAnnotationPresent(Attribute.class)) {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    field.set(copy, value);
                }
            }

            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy AttributeData instance", e);
        }
    }
}
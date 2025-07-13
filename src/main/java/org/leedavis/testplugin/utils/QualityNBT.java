package org.leedavis.testplugin.utils;

import org.bukkit.ChatColor;

public class QualityNBT extends AttributeData {

    @Attribute(key = "quality", displayName = "Quality")
    protected int quality;

    public QualityNBT() {
        this.quality = 0;
    }

    public QualityNBT(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public static String getQualityDisplay(int quality) {
        if (quality <= -5) {
            return ChatColor.DARK_RED + "Terrible (" + quality + ")"; // Dark Red
        } else if (quality <= -3) {
            return ChatColor.RED + "Poor (" + quality + ")"; // Red
        } else if (quality <= -1) {
            return ChatColor.YELLOW + "Below Average (" + quality + ")"; // Yellow
        } else if (quality <= 1) {
            return ChatColor.WHITE + "Average (" + quality + ")"; // White
        } else if (quality <= 3) {
            return ChatColor.GREEN + "Above Average (" + quality + ")"; // Green
        } else if (quality <= 5) {
            return ChatColor.AQUA + "Good (" + quality + ")"; // Aqua
        } else if (quality <= 7) {
            return ChatColor.GOLD + "Excellent (" + quality + ")"; // Gold
        } else {
            return ChatColor.LIGHT_PURPLE + "Perfect (" + quality + ")"; // Light Purple
        }
    }
}
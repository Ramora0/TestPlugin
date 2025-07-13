package org.leedavis.testplugin.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

public class QualityNBT extends AttributeData {

    @Attribute
    protected int quality;

    /**
     * Extra quality bonus that affects production/crafting output but does not
     * get passed down when reproducing/breeding crops. This allows for temporary
     * quality improvements (e.g., from tools, fertilizers) without permanently
     * altering the genetic traits of the crop.
     */
    @Attribute
    protected int extra_quality;

    public QualityNBT() {
        this.quality = 0;
        this.extra_quality = 0;
    }

    public QualityNBT(int quality) {
        this.quality = quality;
        this.extra_quality = 0;
    }

    /**
     * Gets the fundamental quality that affects reproduction and breeding.
     * This is the base genetic quality that gets passed down to offspring.
     * 
     * @return the base quality value
     */
    public int getFundamentalQuality() {
        return quality;
    }

    /**
     * Gets the total effective quality including both fundamental and extra
     * quality.
     * This is used for production, crafting, and other output calculations.
     * 
     * @return the combined quality (fundamental + extra)
     */
    public int getQuality() {
        return quality + extra_quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getExtra_quality() {
        return extra_quality;
    }

    public void setExtra_quality(int extraQuality) {
        this.extra_quality = extraQuality;
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

    /**
     * Override to display quality information at the beginning of the lore
     */
    @Override
    protected List<String> getAdditionalLore() {
        List<String> additionalLore = new ArrayList<>();
        additionalLore.add(getQualityDisplay(getQuality()));
        return additionalLore;
    }
}
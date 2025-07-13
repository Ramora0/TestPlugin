package org.leedavis.testplugin.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class CropNBT extends QualityNBT {

    @Attribute
    private int resistance;

    @Attribute
    private int yield;

    private static final double NO_CHANGE_PROBABILITY = 0.;

    public CropNBT() {
        super();
        this.resistance = 0;
        this.yield = 0;
    }

    public CropNBT(int quality, int resistance, int yield) {
        super(quality);
        this.resistance = resistance;
        this.yield = yield;
    }

    public int getResistance() {
        return resistance;
    }

    public void setResistance(int resistance) {
        this.resistance = resistance;
    }

    public int getYield() {
        return yield;
    }

    public void setYield(int yield) {
        this.yield = yield;
    }

    public void mutate() {
        if (Math.random() < NO_CHANGE_PROBABILITY) {
            return;
        }

        int decreaseField;
        do {
            decreaseField = (int) (Math.random() * 3);
        } while ((decreaseField == 0 && quality <= -5) ||
                (decreaseField == 1 && resistance <= -5) ||
                (decreaseField == 2 && yield <= -5));

        int increaseField;
        do {
            increaseField = (int) (Math.random() * 3);
        } while ((increaseField == 0 && quality >= 5) ||
                (increaseField == 1 && resistance >= 5) ||
                (increaseField == 2 && yield >= 5) ||
                increaseField == decreaseField);

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

    public ItemStack getSeeds() {
        CropNBT copy = copy();
        copy.setExtra_quality(0);

        return copy.imbueToItem(new ItemStack(Material.WHEAT_SEEDS, 1));
    }

    public ItemStack getCropWithHoe(int hoeLevel) {
        int extraYield = 0;
        if (hoeLevel == 1) {
            extraYield++;
            extra_quality--;
        } else if (hoeLevel == 2) {
            extraYield += 2;
            extra_quality -= 1;
        } else if (hoeLevel == 3) {
            extraYield += 2;
            extra_quality += 0;
        } else if (hoeLevel == 4) {
            extraYield += 2;
            extra_quality += 1;
        } else if (hoeLevel != 0) {
            System.out.println("Invalid hoe level: " + hoeLevel);
        }

        double adjYield = Math.pow(2.5, (yield + extraYield) / 5.0 + 1) / 2;
        int amount = 0;
        while (adjYield > 0) {
            if (adjYield > 1) {
                amount++;
            } else if (Math.random() < adjYield) {
                amount++;
            }
            adjYield -= 1.0;
        }

        return imbueToItem(new ItemStack(Material.WHEAT, amount));
    }

    public ItemStack getCrop() {
        return getCropWithHoe(0);
    }

    @Override
    public String toString() {
        return String.format("Quality: %d, Resistance: %d, Yield: %d", quality, resistance, yield);
    }
}
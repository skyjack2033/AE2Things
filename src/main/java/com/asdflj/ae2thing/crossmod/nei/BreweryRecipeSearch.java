package com.asdflj.ae2thing.crossmod.nei;

import net.minecraft.item.ItemStack;

/** Runtime helper outside the Mixin package, which may not be loaded directly. */
public final class BreweryRecipeSearch {

    private BreweryRecipeSearch() {}

    public static boolean matchesItem(ItemStack result, ItemStack filledContainer) {
        // A brew container may reject a brew by returning null. The caller still checks matching NBT afterward.
        return result != null && filledContainer != null && result.isItemEqual(filledContainer);
    }
}

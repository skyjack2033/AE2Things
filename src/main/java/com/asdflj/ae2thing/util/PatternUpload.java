package com.asdflj.ae2thing.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.implementations.ICraftingPatternItem;

public final class PatternUpload {

    private PatternUpload() {}

    /** Moves one pattern only after the destination confirms that it accepted the copy. */
    public static boolean moveToSlot(IInventory source, int sourceSlot, IInventory target, int targetSlot,
        int accessibleSlots) {
        if (source == null || target == null
            || source == target
            || sourceSlot < 0
            || sourceSlot >= source.getSizeInventory()) return false;
        int slots = Math.min(accessibleSlots, target.getSizeInventory());
        if (targetSlot < 0 || targetSlot >= slots || target.getInventoryStackLimit() < 1) return false;

        ItemStack pattern = source.getStackInSlot(sourceSlot);
        if (pattern == null || pattern.stackSize <= 0
            || !(pattern.getItem() instanceof ICraftingPatternItem)
            || target.getStackInSlot(targetSlot) != null) return false;
        ItemStack inserted = pattern.copy();
        inserted.stackSize = 1;
        if (!target.isItemValidForSlot(targetSlot, inserted)) return false;
        for (int slot = 0; slot < slots; slot++) {
            if (samePattern(target.getStackInSlot(slot), inserted)) return false;
        }

        // Some virtual hatch inventories silently refuse writes. Keep the output until readback succeeds.
        target.setInventorySlotContents(targetSlot, inserted.copy());
        if (!ItemStack.areItemStacksEqual(inserted, target.getStackInSlot(targetSlot))) {
            target.setInventorySlotContents(targetSlot, null);
            return false;
        }

        ItemStack remaining = pattern.stackSize == 1 ? null : pattern.copy();
        if (remaining != null) remaining.stackSize--;
        source.setInventorySlotContents(sourceSlot, remaining == null ? null : remaining.copy());
        if (!ItemStack.areItemStacksEqual(remaining, source.getStackInSlot(sourceSlot))) {
            // A source wrapper that refuses removal must not leave a second pattern in the hatch.
            if (!ItemStack.areItemStacksEqual(pattern, source.getStackInSlot(sourceSlot))) {
                source.setInventorySlotContents(sourceSlot, pattern.copy());
            }
            target.setInventorySlotContents(targetSlot, null);
            return false;
        }
        source.markDirty();
        target.markDirty();
        return true;
    }

    private static boolean samePattern(ItemStack first, ItemStack second) {
        return first != null && first.isItemEqual(second) && ItemStack.areItemStackTagsEqual(first, second);
    }
}

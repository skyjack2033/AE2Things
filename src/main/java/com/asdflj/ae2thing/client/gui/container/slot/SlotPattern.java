package com.asdflj.ae2thing.client.gui.container.slot;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.container.slot.SlotRestrictedInput;

public class SlotPattern extends SlotRestrictedInput {

    public SlotPattern(PlacableItemType valid, IInventory i, int slotIndex, int x, int y, InventoryPlayer p) {
        super(valid, i, slotIndex, x, y, p);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (getItemType() == PlacableItemType.UPGRADES) {
            return stack != null && this.inventory.isItemValidForSlot(this.getSlotIndex(), stack);
        }
        return super.isItemValid(stack);
    }
}

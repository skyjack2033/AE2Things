package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;

public class PatternUploadTest {

    private MemoryInventory source;
    private MemoryInventory target;
    private ItemStack pattern;

    @Before
    public void setUp() {
        source = new MemoryInventory(2);
        target = new MemoryInventory(36);
        pattern = new ItemStack(new PatternItem(), 1);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("recipe", "programming circuit");
        pattern.setTagCompound(data);
        source.items[1] = pattern;
    }

    @Test
    public void uploadsAnIndependentCopyToTheLastHatchSlot() {
        assertTrue(PatternUpload.moveToSlot(source, 1, target, 35, 36));
        assertNull(source.items[1]);
        assertTrue(ItemStack.areItemStacksEqual(pattern, target.items[35]));
        assertNotSame(pattern, target.items[35]);
        assertNotSame(pattern.getTagCompound(), target.items[35].getTagCompound());
        assertEquals(1, source.dirtyCount);
        assertEquals(1, target.dirtyCount);
    }

    @Test
    public void movesOnlyOnePatternAndPreservesTheRemainder() {
        pattern.stackSize = 3;
        assertTrue(PatternUpload.moveToSlot(source, 1, target, 0, 36));
        assertEquals(2, source.items[1].stackSize);
        assertEquals(1, target.items[0].stackSize);
    }

    @Test
    public void leavesOccupiedSlotsAndTheOutputUntouched() {
        ItemStack existing = new ItemStack(new Item());
        target.items[4] = existing;
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 4, 36));
        assertSame(pattern, source.items[1]);
        assertSame(existing, target.items[4]);
        assertEquals(0, target.writeCount);
    }

    @Test
    public void boundsUploadsByBothExposedSlotsAndPhysicalInventory() {
        assertFalse(PatternUpload.moveToSlot(source, 1, target, -1, 36));
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 35, 35));
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 36, 40));
        assertFalse(PatternUpload.moveToSlot(source, -1, target, 0, 36));
        assertSame(pattern, source.items[1]);
        assertEquals(0, target.writeCount);
    }

    @Test
    public void rejectsDuplicatePatternsRegardlessOfStackCount() {
        target.items[20] = pattern.copy();
        target.items[20].stackSize = 2;
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 0, 36));
        assertSame(pattern, source.items[1]);
        assertNull(target.items[0]);
        assertEquals(0, target.writeCount);
    }

    @Test
    public void permitsPatternsWithDifferentRecipeNbt() {
        target.items[0] = pattern.copy();
        target.items[0].getTagCompound()
            .setString("recipe", "another circuit");
        assertTrue(PatternUpload.moveToSlot(source, 1, target, 1, 36));
    }

    @Test
    public void preservesOutputWhenTheSlotRejectsPatterns() {
        target.acceptsItems = false;
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 0, 36));
        assertSame(pattern, source.items[1]);
        assertEquals(0, target.writeCount);
    }

    @Test
    public void preservesOutputWhenAVirtualInventorySilentlyRejectsTheWrite() {
        target.rejectWrites = true;
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 0, 36));
        assertSame(pattern, source.items[1]);
        assertNull(target.items[0]);
        assertEquals(0, source.writeCount);
    }

    @Test
    public void rollsBackTargetIfTheSourceSilentlyRefusesRemoval() {
        source.rejectWrites = true;
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 0, 36));
        assertSame(pattern, source.items[1]);
        assertNull(target.items[0]);
    }

    @Test
    public void rejectsNonPatternOutput() {
        source.items[1] = new ItemStack(new Item());
        assertFalse(PatternUpload.moveToSlot(source, 1, target, 0, 36));
        assertEquals(0, target.writeCount);
    }

    private static class PatternItem extends Item implements ICraftingPatternItem {

        @Override
        public ICraftingPatternDetails getPatternForItem(ItemStack stack, World world) {
            return null;
        }
    }

    private static class MemoryInventory implements IInventory {

        private final ItemStack[] items;
        private boolean acceptsItems = true;
        private boolean rejectWrites;
        private int writeCount;
        private int dirtyCount;

        MemoryInventory(int size) {
            items = new ItemStack[size];
        }

        @Override
        public int getSizeInventory() {
            return items.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            writeCount++;
            if (!rejectWrites) items[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return "pattern upload test";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            dirtyCount++;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return acceptsItems;
        }
    }
}

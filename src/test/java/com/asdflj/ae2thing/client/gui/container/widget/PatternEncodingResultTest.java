package com.asdflj.ae2thing.client.gui.container.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Before;
import org.junit.Test;

public class PatternEncodingResultTest {

    private PatternInventory inventory;
    private Slot blankSlot;
    private Slot outputSlot;
    private ItemStack encoded;

    @Before
    public void setUp() {
        inventory = new PatternInventory();
        blankSlot = new Slot(inventory, 0, 0, 0);
        outputSlot = new Slot(inventory, 1, 0, 0);
        encoded = new ItemStack(new Item());
        NBTTagCompound data = new NBTTagCompound();
        data.setString("recipe", "new recipe");
        encoded.setTagCompound(data);
    }

    @Test
    public void failedEncodingDoesNotReuseAnExistingOutput() {
        ItemStack previous = encoded.copy();
        previous.getTagCompound()
            .setString("recipe", "previous recipe");
        outputSlot.putStack(previous);
        ItemStack blanks = new ItemStack(new Item(), 4);
        blankSlot.putStack(blanks);

        assertFalse(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, null));
        assertSame(previous, outputSlot.getStack());
        assertSame(blanks, blankSlot.getStack());
    }

    @Test
    public void newEncodingConsumesExactlyOneBlankAfterTheOutputAcceptsIt() {
        blankSlot.putStack(new ItemStack(new Item(), 4));

        assertTrue(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, encoded));
        assertEquals(3, blankSlot.getStack().stackSize);
        assertTrue(ItemStack.areItemStacksEqual(encoded, outputSlot.getStack()));
        assertNotSame(encoded, outputSlot.getStack());
        assertNotSame(
            encoded.getTagCompound(),
            outputSlot.getStack()
                .getTagCompound());
    }

    @Test
    public void theLastBlankIsRemovedOnlyOnSuccessfulEncoding() {
        blankSlot.putStack(new ItemStack(new Item()));

        assertTrue(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, encoded));
        assertNull(blankSlot.getStack());
    }

    @Test
    public void missingBlankLeavesTheOutputEmpty() {
        assertFalse(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, encoded));
        assertNull(outputSlot.getStack());
    }

    @Test
    public void reencodingReplacesTheOldOutputWithoutConsumingABlank() {
        ItemStack previous = encoded.copy();
        previous.getTagCompound()
            .setString("recipe", "previous recipe");
        outputSlot.putStack(previous);
        ItemStack blanks = new ItemStack(new Item(), 4);
        blankSlot.putStack(blanks);

        assertTrue(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, encoded));
        assertEquals(
            "new recipe",
            outputSlot.getStack()
                .getTagCompound()
                .getString("recipe"));
        assertEquals(
            "previous recipe",
            previous.getTagCompound()
                .getString("recipe"));
        assertSame(blanks, blankSlot.getStack());
    }

    @Test
    public void rejectedOutputPreservesThePreviousPatternAndBlanks() {
        ItemStack previous = encoded.copy();
        previous.getTagCompound()
            .setString("recipe", "previous recipe");
        outputSlot.putStack(previous);
        ItemStack blanks = new ItemStack(new Item(), 4);
        blankSlot.putStack(blanks);
        inventory.rejectedSlot = 1;

        assertFalse(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, encoded));
        assertSame(previous, outputSlot.getStack());
        assertSame(blanks, blankSlot.getStack());
    }

    @Test
    public void rejectedBlankConsumptionRollsBackTheNewOutput() {
        ItemStack blanks = new ItemStack(new Item(), 4);
        blankSlot.putStack(blanks);
        inventory.rejectedSlot = 0;

        assertFalse(PatternContainer.storeEncodedPattern(blankSlot, outputSlot, encoded));
        assertSame(blanks, blankSlot.getStack());
        assertNull(outputSlot.getStack());
    }

    private static class PatternInventory extends InventoryBasic {

        private int rejectedSlot = -1;

        PatternInventory() {
            super("pattern encoding test", false, 2);
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot != rejectedSlot) super.setInventorySlotContents(slot, stack);
        }
    }
}

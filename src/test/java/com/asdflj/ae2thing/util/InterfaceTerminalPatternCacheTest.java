package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import appeng.api.util.DimensionalCoord;
import appeng.api.util.IInterfaceViewable;

public class InterfaceTerminalPatternCacheTest {

    @Test
    public void repeatedCapturesReuseUnchangedNbtButKeepTrackerListsIndependent() {
        IInterfaceViewable host = host();
        MemoryInventory inventory = new MemoryInventory(2);
        inventory.items[0] = pattern("first");
        InterfaceTerminalPatternCache.Snapshot first = InterfaceTerminalPatternCache.capture(host, inventory, 2);
        NBTTagList firstList = first.copyItemList();
        ItemStack[] firstPatterns = first.copyPatternReferences();

        InterfaceTerminalPatternCache.Snapshot reopened = InterfaceTerminalPatternCache.capture(host, inventory, 2);
        NBTTagList reopenedList = reopened.copyItemList();
        ItemStack[] reopenedPatterns = reopened.copyPatternReferences();

        assertNotSame(firstList, reopenedList);
        assertSame(firstList.getCompoundTagAt(0), reopenedList.getCompoundTagAt(0));
        assertSame(firstList.getCompoundTagAt(1), reopenedList.getCompoundTagAt(1));
        assertNotSame(firstPatterns, reopenedPatterns);
        assertSame(firstPatterns[0], reopenedPatterns[0]);
        firstList.func_150304_a(0, new NBTTagCompound());
        assertEquals("first", recipe(reopenedList, 0));
    }

    @Test
    public void inPlacePatternNbtChangesLeaveOldSnapshotsUntouchedAndAreFoundOnReopen() {
        IInterfaceViewable host = host();
        MemoryInventory inventory = new MemoryInventory(1);
        inventory.items[0] = pattern("first");
        InterfaceTerminalPatternCache.Snapshot first = InterfaceTerminalPatternCache.capture(host, inventory, 1);
        NBTTagList firstList = first.copyItemList();
        ItemStack[] firstPatterns = first.copyPatternReferences();

        inventory.items[0].getTagCompound()
            .setString("recipe", "changed");

        assertEquals("first", recipe(firstList, 0));
        assertEquals(
            "first",
            firstPatterns[0].getTagCompound()
                .getString("recipe"));

        InterfaceTerminalPatternCache.Snapshot reopened = InterfaceTerminalPatternCache.capture(host, inventory, 1);
        NBTTagList reopenedList = reopened.copyItemList();
        ItemStack[] reopenedPatterns = reopened.copyPatternReferences();

        assertEquals("changed", recipe(reopenedList, 0));
        assertEquals("first", recipe(firstList, 0));
        assertNotSame(firstList.getCompoundTagAt(0), reopenedList.getCompoundTagAt(0));
        assertNotSame(firstPatterns[0], reopenedPatterns[0]);
        assertNotSame(inventory.items[0].getTagCompound(), reopenedPatterns[0].getTagCompound());
    }

    @Test
    public void aSingleSlotUpdateDoesNotMutateAnotherTrackersInitialList() {
        IInterfaceViewable host = host();
        MemoryInventory inventory = new MemoryInventory(3);
        inventory.items[0] = pattern("unchanged");
        inventory.items[1] = pattern("first");
        InterfaceTerminalPatternCache.Snapshot snapshot = InterfaceTerminalPatternCache.capture(host, inventory, 3);
        NBTTagList firstTracker = snapshot.copyItemList();
        NBTTagList secondTracker = snapshot.copyItemList();
        ItemStack[] firstPatterns = snapshot.copyPatternReferences();

        inventory.items[1] = pattern("changed");
        NBTTagCompound changed = InterfaceTerminalPatternCache.updateSlot(host, inventory, 3, 1);
        secondTracker.func_150304_a(1, changed);

        NBTTagList reopened = InterfaceTerminalPatternCache.capture(host, inventory, 3)
            .copyItemList();
        assertEquals("first", recipe(firstTracker, 1));
        assertEquals("changed", recipe(secondTracker, 1));
        assertEquals("changed", recipe(reopened, 1));
        assertEquals(
            "first",
            firstPatterns[1].getTagCompound()
                .getString("recipe"));
        assertNotSame(firstTracker.getCompoundTagAt(1), changed);
        assertSame(changed, reopened.getCompoundTagAt(1));
        assertSame(firstTracker.getCompoundTagAt(0), reopened.getCompoundTagAt(0));
        assertSame(firstTracker.getCompoundTagAt(2), reopened.getCompoundTagAt(2));
    }

    @Test
    public void captureResizesAndRespectsTheExposed360SlotBoundary() {
        IInterfaceViewable host = host();
        MemoryInventory inventory = new MemoryInventory(364);
        inventory.items[359] = pattern("last pattern");
        inventory.items[360] = pattern("manual input");

        NBTTagList patterns = InterfaceTerminalPatternCache.capture(host, inventory, 360)
            .copyItemList();
        assertEquals(360, patterns.tagCount());
        assertEquals("last pattern", recipe(patterns, 359));

        NBTTagList reduced = InterfaceTerminalPatternCache.capture(host, inventory, 359)
            .copyItemList();
        assertEquals(359, reduced.tagCount());
        assertEquals(360, patterns.tagCount());

        NBTTagList physical = InterfaceTerminalPatternCache.capture(host, new MemoryInventory(9), 360)
            .copyItemList();
        assertEquals(9, physical.tagCount());
        assertEquals(
            0,
            InterfaceTerminalPatternCache.capture(host, inventory, -1)
                .copyItemList()
                .tagCount());
    }

    @Test
    public void differentHostsAtTheSameLocationNeverShareSnapshots() {
        IInterfaceViewable firstHost = host();
        IInterfaceViewable secondHost = host();
        assertEquals(firstHost, secondHost);
        assertEquals(firstHost.getLocation(), secondHost.getLocation());
        MemoryInventory firstInventory = new MemoryInventory(1);
        MemoryInventory secondInventory = new MemoryInventory(1);
        firstInventory.items[0] = pattern("first host");
        secondInventory.items[0] = pattern("second host");

        NBTTagList first = InterfaceTerminalPatternCache.capture(firstHost, firstInventory, 1)
            .copyItemList();
        NBTTagList second = InterfaceTerminalPatternCache.capture(secondHost, secondInventory, 1)
            .copyItemList();

        assertEquals("first host", recipe(first, 0));
        assertEquals("second host", recipe(second, 0));
        assertNotSame(first.getCompoundTagAt(0), second.getCompoundTagAt(0));
        assertSame(
            first.getCompoundTagAt(0),
            InterfaceTerminalPatternCache.capture(firstHost, firstInventory, 1)
                .copyItemList()
                .getCompoundTagAt(0));
    }

    @Test
    public void singleSlotCacheMissDoesNotCreateAPartiallyInitializedSnapshot() {
        IInterfaceViewable host = host();
        MemoryInventory inventory = new MemoryInventory(3);
        inventory.items[0] = pattern("first");
        inventory.items[1] = pattern("second");

        NBTTagCompound firstUpdate = InterfaceTerminalPatternCache.updateSlot(host, inventory, 3, 1);
        NBTTagCompound secondUpdate = InterfaceTerminalPatternCache.updateSlot(host, inventory, 3, 1);
        assertNotSame(firstUpdate, secondUpdate);

        NBTTagList captured = InterfaceTerminalPatternCache.capture(host, inventory, 3)
            .copyItemList();
        assertEquals(3, captured.tagCount());
        assertEquals("first", recipe(captured, 0));
        assertEquals("second", recipe(captured, 1));
        assertEquals(new NBTTagCompound(), captured.getCompoundTagAt(2));
    }

    private static IInterfaceViewable host() {
        return (IInterfaceViewable) Proxy.newProxyInstance(
            InterfaceTerminalPatternCacheTest.class.getClassLoader(),
            new Class<?>[] { IInterfaceViewable.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getLocation" -> new DimensionalCoord(10, 64, 20, 0);
            case "equals" -> args[0] instanceof IInterfaceViewable;
            case "hashCode" -> 1;
            case "toString" -> "pattern cache host";
            default -> throw new AssertionError("Unexpected method: " + method.getName());
            });
    }

    private static ItemStack pattern(String recipe) {
        ItemStack stack = new ItemStack(new Item());
        NBTTagCompound data = new NBTTagCompound();
        data.setString("recipe", recipe);
        stack.setTagCompound(data);
        return stack;
    }

    private static String recipe(NBTTagList list, int slot) {
        return list.getCompoundTagAt(slot)
            .getCompoundTag("tag")
            .getString("recipe");
    }

    private static final class MemoryInventory implements IInventory {

        private final ItemStack[] items;

        private MemoryInventory(int size) {
            this.items = new ItemStack[size];
        }

        @Override
        public int getSizeInventory() {
            return this.items.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.items[slot];
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
            this.items[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return "pattern cache test";
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
        public void markDirty() {}

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
            return true;
        }
    }
}

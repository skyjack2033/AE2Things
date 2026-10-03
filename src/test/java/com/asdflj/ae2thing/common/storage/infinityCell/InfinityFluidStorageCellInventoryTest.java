package com.asdflj.ae2thing.common.storage.infinityCell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.UUID;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.common.storage.DataStorage;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;

public class InfinityFluidStorageCellInventoryTest {

    @Test
    public void clientQueriesReturnEmptyWithoutTouchingDiskNbtOrOutputList() throws Exception {
        ItemStack cell = new ItemStack(new CellItem());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(
            Constants.DISKUUID,
            UUID.randomUUID()
                .toString());
        tag.setLong("ft", 2);
        tag.setLong("fc", 8000);
        cell.setTagCompound(tag);
        NBTTagCompound before = (NBTTagCompound) tag.copy();
        InfinityFluidStorageCellInventory inventory = new InfinityFluidStorageCellInventory(
            cell,
            null,
            ignored -> null);
        IItemList<IAEFluidStack> out = rejectingList();

        assertSame(out, inventory.getAvailableItems(out, 1));
        assertNull(inventory.getAvailableItem(fluid(1000), 1));
        assertTrue(
            inventory.getContents()
                .isEmpty());
        assertEquals(before, cell.getTagCompound());
        assertEquals(before.getString(Constants.DISKUUID), inventory.getUUID());
        assertEquals(2, inventory.getStoredItemTypes());
        assertEquals(8000, inventory.getStoredItemCount());
    }

    @Test
    public void clientLookupDoesNotInitializeAnUnusedDisk() throws Exception {
        ItemStack cell = new ItemStack(new CellItem());
        InfinityFluidStorageCellInventory inventory = new InfinityFluidStorageCellInventory(
            cell,
            null,
            ignored -> null);

        inventory.getAvailableItems(rejectingList(), 1);
        assertTrue(
            inventory.getContents()
                .isEmpty());
        assertEquals("", inventory.getUUID());
        assertNull(cell.getTagCompound());
    }

    @Test
    public void missingStorageRejectsBothSimulatedAndRealTransfers() throws Exception {
        ItemStack cell = new ItemStack(new CellItem());
        InfinityFluidStorageCellInventory inventory = new InfinityFluidStorageCellInventory(
            cell,
            null,
            ignored -> null);
        IAEFluidStack stack = fluid(1000);

        for (Actionable mode : Actionable.values()) {
            assertSame(stack, inventory.injectItems(stack, mode, null));
            assertNull(inventory.extractItems(stack, mode, null));
        }
        assertEquals(1000, stack.getStackSize());
        assertNull(cell.getTagCompound());
        assertNull(inventory.cellFluids);
    }

    @Test
    public void serverQueriesKeepTheOriginalBackendAndReturnCopies() throws Exception {
        ItemStack cell = new ItemStack(new CellItem());
        IAEFluidStack stored = fluid(8000);
        IItemList<IAEFluidStack> fluids = singleFluidList(stored);
        DataStorage storage = new DataStorage(UUID.randomUUID(), StorageChannel.FLUIDS) {

            @Override
            public IItemList<IAEFluidStack> getFluids() {
                return fluids;
            }
        };
        InfinityFluidStorageCellInventory inventory = new InfinityFluidStorageCellInventory(
            cell,
            null,
            ignored -> storage);

        assertSame(storage, inventory.storage);
        assertSame(fluids, inventory.getCellFluids());
        assertSame(
            stored,
            inventory.getContents()
                .get(0));
        assertEquals(storage.getUUID(), inventory.getUUID());
        assertEquals(
            storage.getUUID(),
            cell.getTagCompound()
                .getString(Constants.DISKUUID));

        IAEFluidStack available = inventory.getAvailableItem(fluid(1000), 1);
        assertNotSame(stored, available);
        assertEquals(8000, available.getStackSize());
        IAEFluidStack extracted = inventory.extractItems(fluid(1000), Actionable.SIMULATE, null);
        assertEquals(1000, extracted.getStackSize());
        assertEquals(8000, stored.getStackSize());
    }

    @Test
    public void keepsTheNbtInitializedByTheServerStorageResolver() throws Exception {
        ItemStack cell = new ItemStack(new CellItem());
        NBTTagCompound initialized = new NBTTagCompound();
        initialized.setLong("fc", 4000);
        DataStorage storage = new DataStorage(UUID.randomUUID(), StorageChannel.FLUIDS);

        InfinityFluidStorageCellInventory inventory = new InfinityFluidStorageCellInventory(cell, null, owner -> {
            assertSame(cell, owner.getItemStack());
            cell.setTagCompound(initialized);
            return storage;
        });

        assertSame(initialized, inventory.data);
        assertSame(initialized, cell.getTagCompound());
        assertEquals(4000, inventory.getStoredItemCount());
    }

    @SuppressWarnings("unchecked")
    private static IItemList<IAEFluidStack> rejectingList() {
        return (IItemList<IAEFluidStack>) Proxy.newProxyInstance(
            IItemList.class.getClassLoader(),
            new Class<?>[] { IItemList.class },
            (proxy, method, args) -> {
                throw new AssertionError("Client lookup must leave the output list untouched: " + method.getName());
            });
    }

    @SuppressWarnings("unchecked")
    private static IItemList<IAEFluidStack> singleFluidList(IAEFluidStack stored) {
        return (IItemList<IAEFluidStack>) Proxy.newProxyInstance(
            IItemList.class.getClassLoader(),
            new Class<?>[] { IItemList.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "iterator" -> Collections.singletonList(stored)
                .iterator();
            case "findPrecise" -> stored;
            default -> throw new AssertionError("Unexpected list operation: " + method.getName());
            });
    }

    private static IAEFluidStack fluid(long amount) {
        long[] size = { amount };
        return (IAEFluidStack) Proxy.newProxyInstance(
            IAEFluidStack.class.getClassLoader(),
            new Class<?>[] { IAEFluidStack.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getStackSize" -> size[0];
            case "copy" -> fluid(size[0]);
            case "setStackSize" -> {
            size[0] = (long) args[0];
            yield proxy;
            }
            default -> throw new AssertionError("Unexpected fluid operation: " + method.getName());
            });
    }

    private static class CellItem extends Item implements IStorageCell {

        @Override
        public int getBytes(ItemStack cellItem) {
            return Integer.MAX_VALUE;
        }

        @Override
        public int BytePerType(ItemStack cellItem) {
            return 1;
        }

        @Override
        public int getBytesPerType(ItemStack cellItem) {
            return 1;
        }

        @Override
        public int getTotalTypes(ItemStack cellItem) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean storableInStorageCell() {
            return false;
        }

        @Override
        public boolean isStorageCell(ItemStack cellItem) {
            return true;
        }

        @Override
        public double getIdleDrain() {
            return 0;
        }

        @Override
        public boolean isEditable(ItemStack cellItem) {
            return false;
        }

        @Override
        public IInventory getUpgradesInventory(ItemStack cellItem) {
            return null;
        }

        @Override
        public FuzzyMode getFuzzyMode(ItemStack cellItem) {
            return FuzzyMode.IGNORE_ALL;
        }

        @Override
        public void setFuzzyMode(ItemStack cellItem, FuzzyMode mode) {}
    }
}

package com.asdflj.ae2thing.util;

import java.util.concurrent.TimeUnit;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import appeng.api.util.IInterfaceViewable;

public final class InterfaceTerminalPatternCache {

    private static final Cache<IInterfaceViewable, Snapshot> CACHE = CacheBuilder.newBuilder()
        .weakKeys()
        .maximumWeight(32768)
        .weigher((IInterfaceViewable host, Snapshot snapshot) -> Math.max(1, snapshot.patterns.length))
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();

    private InterfaceTerminalPatternCache() {}

    public static Snapshot capture(IInterfaceViewable host, IInventory inventory, int slots) {
        int size = Math.max(0, Math.min(slots, inventory.getSizeInventory()));
        Snapshot snapshot = CACHE.getIfPresent(host);
        if (snapshot == null || snapshot.patterns.length != size) {
            snapshot = new Snapshot(size);
            CACHE.put(host, snapshot);
        }
        for (int slot = 0; slot < size; slot++) {
            snapshot.update(slot, inventory.getStackInSlot(slot));
        }
        return snapshot;
    }

    public static NBTTagCompound updateSlot(IInterfaceViewable host, IInventory inventory, int slots, int slot) {
        ItemStack current = inventory.getStackInSlot(slot);
        Snapshot snapshot = CACHE.getIfPresent(host);
        if (snapshot != null && snapshot.patterns.length == slots) {
            return snapshot.update(slot, current);
        }
        return serialize(ItemStack.copyItemStack(current));
    }

    private static NBTTagCompound serialize(ItemStack pattern) {
        NBTTagCompound item = new NBTTagCompound();
        if (pattern != null) pattern.writeToNBT(item);
        return item;
    }

    public static final class Snapshot {

        private final ItemStack[] patterns;
        private final NBTTagCompound[] items;

        private Snapshot(int size) {
            this.patterns = new ItemStack[size];
            this.items = new NBTTagCompound[size];
        }

        private NBTTagCompound update(int slot, ItemStack current) {
            if (this.items[slot] == null || !ItemStack.areItemStacksEqual(this.patterns[slot], current)) {
                this.patterns[slot] = ItemStack.copyItemStack(current);
                this.items[slot] = serialize(this.patterns[slot]);
            }
            return this.items[slot];
        }

        public NBTTagList copyItemList() {
            NBTTagList list = new NBTTagList();
            for (NBTTagCompound item : this.items) {
                list.appendTag(item);
            }
            return list;
        }

        public ItemStack[] copyPatternReferences() {
            return this.patterns.clone();
        }
    }
}

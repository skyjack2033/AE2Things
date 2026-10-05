package com.asdflj.ae2thing.inventory;

import java.util.Collections;
import java.util.Iterator;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import com.asdflj.ae2thing.integration.Mods;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import appeng.util.item.AEItemStack;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Inventory adaptor for Programmable Hatches' dual input hatches.
 *
 * <p>
 * AE2 converts a fluid pattern input to an ItemFluidPacket when the target
 * is not an AE2 fluid interface. Programmable Hatches expose both an item
 * inventory and fluid tanks, so the packet must be decoded back into a fluid
 * before insertion. The same conversion is needed for queued interface
 * outputs, where AE2 passes the native IAEFluidStack directly.
 * </p>
 */
public final class ProgrammableHatchInventoryAdaptor extends InventoryAdaptor {

    private static final String DUAL_INPUT_HATCH = "reobf.proghatches.gt.metatileentity.DualInputHatch";

    private final InventoryAdaptor itemAdaptor;
    private final BaseMetaTileEntity fluidHandler;
    private final ForgeDirection side;

    private ProgrammableHatchInventoryAdaptor(TileEntity target, ForgeDirection side) {
        this.itemAdaptor = InventoryAdaptor.getAdaptor(target, side, InventoryAdaptor.ALLOW_ITEMS);
        this.fluidHandler = (BaseMetaTileEntity) target;
        this.side = side;
    }

    /**
     * Returns a PH adaptor only for the p26 dual input hatches. Keeping this
     * check here prevents the fluid packet behaviour from changing ordinary
     * GT input buses or unrelated fluid handlers.
     */
    @Nullable
    public static InventoryAdaptor tryCreate(TileEntity target, ForgeDirection side) {
        if (!Mods.PROGRAMMABLE_HATCHES.isModLoaded() || target == null
            || side == null
            || !(target instanceof BaseMetaTileEntity)) return null;

        BaseMetaTileEntity base = (BaseMetaTileEntity) target;
        if (base.getMetaTileEntity() == null) return null;
        String name = base.getMetaTileEntity()
            .getClass()
            .getName();
        // Includes p26's BufferedDualInputHatch, DualInputHachOC and
        // DualInputHatchSlave variants, which share the same fluid bridge.
        if (!name.startsWith(DUAL_INPUT_HATCH)) return null;
        return new ProgrammableHatchInventoryAdaptor(target, side);
    }

    @Override
    public ItemStack removeItems(int amount, ItemStack filter, IInventoryDestination destination) {
        return itemAdaptor == null ? null : itemAdaptor.removeItems(amount, filter, destination);
    }

    @Override
    public ItemStack simulateRemove(int amount, ItemStack filter, IInventoryDestination destination) {
        return itemAdaptor == null ? null : itemAdaptor.simulateRemove(amount, filter, destination);
    }

    @Override
    public ItemStack removeSimilarItems(int amount, ItemStack filter, FuzzyMode fuzzyMode,
        IInventoryDestination destination) {
        return itemAdaptor == null ? null : itemAdaptor.removeSimilarItems(amount, filter, fuzzyMode, destination);
    }

    @Override
    public ItemStack simulateSimilarRemove(int amount, ItemStack filter, FuzzyMode fuzzyMode,
        IInventoryDestination destination) {
        return itemAdaptor == null ? null : itemAdaptor.simulateSimilarRemove(amount, filter, fuzzyMode, destination);
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded) {
        return itemAdaptor == null ? toBeAdded : itemAdaptor.addItems(toBeAdded);
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded, InsertionMode insertionMode) {
        return itemAdaptor == null ? toBeAdded : itemAdaptor.addItems(toBeAdded, insertionMode);
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated) {
        return itemAdaptor == null ? toBeSimulated : itemAdaptor.simulateAdd(toBeSimulated);
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated, InsertionMode insertionMode) {
        return itemAdaptor == null ? toBeSimulated : itemAdaptor.simulateAdd(toBeSimulated, insertionMode);
    }

    @Override
    public IAEStack<?> addStack(IAEStack<?> toBeAdded, InsertionMode insertionMode) {
        IAEFluidStack fluid = asFluid(toBeAdded);
        if (fluid != null) return insertFluid(toBeAdded, fluid, true);
        return itemAdaptor == null ? toBeAdded : itemAdaptor.addStack(toBeAdded, insertionMode);
    }

    @Override
    public IAEStack<?> simulateAddStack(IAEStack<?> toBeSimulated, InsertionMode insertionMode) {
        IAEFluidStack fluid = asFluid(toBeSimulated);
        if (fluid != null) return insertFluid(toBeSimulated, fluid, false);
        return itemAdaptor == null ? toBeSimulated : itemAdaptor.simulateAddStack(toBeSimulated, insertionMode);
    }

    @Nullable
    private static IAEFluidStack asFluid(IAEStack<?> stack) {
        if (stack instanceof IAEFluidStack fluid) return fluid;
        if (stack instanceof IAEItemStack item && item.getItem() instanceof ItemFluidPacket) {
            return ItemFluidPacket.getFluidAEStack(item);
        }
        return null;
    }

    @Nullable
    private IAEStack<?> insertFluid(IAEStack<?> original, IAEFluidStack fluid, boolean modulate) {
        FluidStack fs = fluid.getFluidStack();
        if (fs == null || fluid.getStackSize() <= 0) return original;

        long requested = fluid.getStackSize();
        int amount = (int) Math.min(Integer.MAX_VALUE, requested);
        FluidStack operation = fs.copy();
        operation.amount = amount;
        int accepted = this.fluidHandler.fill(this.side, operation, modulate);
        if (accepted <= 0) return original;

        long remaining = requested - accepted;
        if (remaining <= 0) return null;
        return stackWithSize(original, fluid, remaining);
    }

    private static IAEStack<?> stackWithSize(IAEStack<?> original, IAEFluidStack fluid, long amount) {
        if (original instanceof IAEItemStack item && item.getItem() instanceof ItemFluidPacket) {
            ItemStack packet = ItemFluidPacket.newStack(fluid.getFluidStack());
            if (packet == null) return original;
            ItemFluidPacket.setFluidAmount(packet, amount);
            return AEItemStack.create(packet);
        }
        IAEFluidStack copy = fluid.copy();
        copy.setStackSize(amount);
        return copy;
    }

    @Override
    public boolean containsItems() {
        return (itemAdaptor != null && itemAdaptor.containsItems()) || containsFluid();
    }

    private boolean containsFluid() {
        net.minecraftforge.fluids.FluidTankInfo[] infos = this.fluidHandler.getTankInfo(this.side);
        if (infos == null) return false;
        for (var info : infos) {
            if (info != null && info.fluid != null && info.fluid.amount > 0) return true;
        }
        return false;
    }

    @Override
    public Iterator<ItemSlot> iterator() {
        Iterator<ItemSlot> items = itemAdaptor == null ? Collections.emptyIterator() : itemAdaptor.iterator();
        FluidTankInfo[] infos = this.fluidHandler.getTankInfo(this.side);
        Iterator<ItemSlot> fluidSlots = fluidIterator(infos);
        int fluidCount = infos == null ? 0 : infos.length;
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return fluidSlots.hasNext() || items.hasNext();
            }

            @Override
            public ItemSlot next() {
                if (fluidSlots.hasNext()) return fluidSlots.next();
                ItemSlot item = items.next();
                item.setSlot(item.getSlot() + fluidCount);
                return item;
            }
        };
    }

    private static Iterator<ItemSlot> fluidIterator(FluidTankInfo[] infos) {
        if (infos == null) return Collections.emptyIterator();
        return new Iterator<>() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < infos.length;
            }

            @Override
            public ItemSlot next() {
                FluidTankInfo info = infos[index];
                ItemSlot slot = new ItemSlot();
                slot.setSlot(index++);
                slot.setItemStack(info == null || info.fluid == null ? null : ItemFluidPacket.newStack(info.fluid));
                slot.setExtractable(false);
                return slot;
            }
        };
    }
}

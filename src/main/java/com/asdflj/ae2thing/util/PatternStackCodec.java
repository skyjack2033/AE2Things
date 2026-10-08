package com.asdflj.ae2thing.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.item.ItemFluidDisplay;

/** Converts the terminal's item-backed slots to the native item/fluid pattern format. */
public final class PatternStackCodec {

    private PatternStackCodec() {}

    public static IAEStack<?> toPatternStack(ItemStack stack) {
        if (stack == null) return null;
        return normalize(AEItemStack.create(stack));
    }

    public static IAEStack<?> normalize(IAEStack<?> stack) {
        if (!(stack instanceof IAEItemStack item)) return stack;

        // Only virtual fluid markers become native fluids; real buckets and cells stay items.
        IAEFluidStack fluid = null;
        boolean fluidMarker = false;
        if (item.getItem() instanceof ItemFluidDrop) {
            fluidMarker = true;
            fluid = ItemFluidDrop.getAeFluidStack(item);
        } else if (item.getItem() instanceof ItemFluidPacket) {
            fluidMarker = true;
            fluid = ItemFluidPacket.getFluidAEStack(item);
        } else if (item.getItem() instanceof ItemFluidDisplay display) {
            fluidMarker = true;
            ItemStack displayed = item.getItemStack();
            fluid = AEFluidStack.create(display.getFluid(displayed));
            if (fluid != null) fluid.setStackSize(display.getAmountLong(displayed));
        }
        if (fluid == null) return fluidMarker ? null : stack;

        fluid.setCraftable(item.isCraftable());
        fluid.setCountRequestable(item.getCountRequestable());
        fluid.setCountRequestableCrafts(item.getCountRequestableCrafts());
        fluid.setUsedPercent(item.getUsedPercent());
        return fluid;
    }

    public static boolean isFluidMarker(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof ItemFluidDrop || stack.getItem() instanceof ItemFluidPacket
            || stack.getItem() instanceof ItemFluidDisplay);
    }

    public static ItemStack toTerminalStack(IAEStack<?> stack) {
        IAEStack<?> nativeStack = normalize(stack);
        if (nativeStack instanceof IAEFluidStack fluid) return ItemFluidPacket.newStack(fluid);
        if (nativeStack instanceof IAEItemStack item) return item.getItemStack();
        return null;
    }

    public static NBTTagCompound processingData(IAEStack<?>[] inputs, IAEStack<?>[] outputs, boolean substitute,
        boolean beSubstitute, boolean prioritize) {
        NBTTagList in = writeStacks(inputs, true);
        NBTTagList out = writeStacks(outputs, false);
        if (in == null || out == null) return null;
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("in", in);
        data.setTag("out", out);
        data.setBoolean("crafting", false);
        data.setBoolean("substitute", substitute);
        data.setBoolean("beSubstitute", beSubstitute);
        data.setBoolean("prioritize", prioritize);
        return data;
    }

    private static NBTTagList writeStacks(IAEStack<?>[] stacks, boolean keepEmptySlots) {
        if (stacks == null) return null;
        NBTTagList list = new NBTTagList();
        boolean hasStack = false;
        for (IAEStack<?> stack : stacks) {
            if (stack == null) {
                if (keepEmptySlots) list.appendTag(new NBTTagCompound());
                continue;
            }
            IAEStack<?> nativeStack = normalize(stack);
            if (nativeStack == null || nativeStack.getStackSize() <= 0) return null;
            list.appendTag(nativeStack.toNBTGeneric());
            hasStack = true;
        }
        return hasStack ? list : null;
    }
}

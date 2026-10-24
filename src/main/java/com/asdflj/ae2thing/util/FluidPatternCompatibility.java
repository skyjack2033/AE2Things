package com.asdflj.ae2thing.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.UltimatePatternHelper;
import appeng.util.Platform;

/** Reads processing patterns whose virtual fluid inputs were accidentally encoded as items. */
public final class FluidPatternCompatibility {

    private FluidPatternCompatibility() {}

    public static ItemStack normalizedCopy(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) return null;
        NBTTagCompound original = pattern.getTagCompound();
        if (original.getBoolean("crafting") || original.getBoolean("tunnel")) return null;

        NBTTagCompound normalized = (NBTTagCompound) original.copy();
        boolean[] changed = { false };
        boolean[] invalid = { false };
        normalized.setTag("in", normalizeEntries(original.getTagList("in", NBT.TAG_COMPOUND), changed, invalid));
        normalized.setTag("out", normalizeEntries(original.getTagList("out", NBT.TAG_COMPOUND), changed, invalid));
        if (!changed[0]) return null;

        if (invalid[0]) normalized.setBoolean("InvalidPattern", true);
        else normalized.removeTag("InvalidPattern");
        ItemStack copy = pattern.copy();
        copy.setTagCompound(normalized);
        return copy;
    }

    private static NBTTagList normalizeEntries(NBTTagList entries, boolean[] changed, boolean[] invalid) {
        NBTTagList result = new NBTTagList();
        for (int slot = 0; slot < entries.tagCount(); slot++) {
            NBTTagCompound entry = entries.getCompoundTagAt(slot);
            IAEStack<?> stack = Platform.readStackNBT(entry);
            if (stack instanceof IAEItemStack item && PatternStackCodec.isFluidMarker(item.getItemStack())) {
                changed[0] = true;
                // Old item-only patterns use Count, whereas generic AE entries use the long Cnt.
                if (!entry.hasKey("Cnt", NBT.TAG_ANY_NUMERIC)) {
                    ItemStack legacy = Platform.loadItemStackFromNBT(entry);
                    if (legacy != null) item.setStackSize(legacy.stackSize);
                }
                IAEStack<?> fluid = PatternStackCodec.normalize(item);
                if (fluid != null && fluid.getStackSize() > 0) {
                    result.appendTag(fluid.toNBTGeneric());
                    continue;
                }
                invalid[0] = true;
            }
            result.appendTag(entry.copy());
        }
        return result;
    }

    public static ICraftingPatternDetails parse(ItemStack original, ItemStack normalized) {
        try {
            return new UltimatePatternHelper(normalized) {

                @Override
                public ItemStack getPattern() {
                    // Interface providers use reference equality to keep a parsed pattern attached to its slot.
                    return original;
                }
            };
        } catch (RuntimeException e) {
            return null;
        }
    }
}

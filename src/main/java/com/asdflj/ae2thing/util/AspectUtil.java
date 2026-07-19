package com.asdflj.ae2thing.util;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.integration.tc.EssentiaItemContainerHelper;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/**
 * Compatibility helper that replaces the removed AE2FluidCraft AspectUtil. In newer ThaumicEnergistics essentia is no
 * longer modeled as a Forge fluid; it is the IAEStack type {@link AEEssentiaStack}. This helper centralizes the
 * aspect/essentia conversion and container queries against the new API so the rest of the mod depends on a single
 * AE2Things owned entry point.
 */
public final class AspectUtil {

    /**
     * Amount of essentia represented by a single item form unit (phial).
     */
    public static final int ESSENTIA_PER_ITEM = AEEssentiaStackType.ESSENTIA_STACK_TYPE.getAmountPerUnit();

    private static final EssentiaItemContainerHelper ESSENTIA_CONTAINER_HELPER = EssentiaItemContainerHelper.INSTANCE;

    private AspectUtil() {}

    public static boolean isEssentiaContainer(ItemStack stack) {
        return (stack != null) && AEEssentiaStackType.ESSENTIA_STACK_TYPE.isContainerItemForType(stack);
    }

    public static boolean isEmptyEssentiaContainer(ItemStack stack) {
        return (stack != null) && ESSENTIA_CONTAINER_HELPER.isContainerEmpty(stack);
    }

    public static ItemStack createEmptyPhial() {
        return ESSENTIA_CONTAINER_HELPER.createEmptyPhial();
    }

    @Nullable
    public static Aspect getAspectFromJar(ItemStack stack) {
        if (stack == null) return null;
        return ESSENTIA_CONTAINER_HELPER.getAspectInContainer(stack);
    }

    @Nullable
    public static AEEssentiaStack getEssentiaFromContainer(ItemStack stack) {
        if (stack == null) return null;
        return AEEssentiaStackType.ESSENTIA_STACK_TYPE.getStackFromContainerItem(stack);
    }

    public static AEEssentiaStack newEssentiaStack(Aspect aspect, long amount) {
        return new AEEssentiaStack(aspect, amount);
    }

    /**
     * The essentia ME monitor backing a network, or null when the grid has no storage cache.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static IMEMonitor<AEEssentiaStack> getEssentiaMonitor(IGrid grid) {
        if (grid == null) return null;
        IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
        if (storageGrid == null) return null;
        return (IMEMonitor<AEEssentiaStack>) storageGrid.getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
    }

    /**
     * Render an aspect amount in a GUI slot using the essentia stack's own renderer.
     */
    public static void drawAspect(EntityPlayer player, int x, int y, Aspect aspect, long amount) {
        if (aspect == null) return;
        new AEEssentiaStack(aspect, (amount <= 0) ? 1 : amount).drawInGui(Minecraft.getMinecraft(), x, y);
    }
}

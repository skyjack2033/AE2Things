package com.asdflj.ae2thing.integration.gtnl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AELog;
import appeng.items.misc.ItemEncodedPattern;
import cpw.mods.fml.common.Loader;

/** Optional bridge to GTNL's native matrix upload API, without a required GTNL dependency. */
public final class AssemblerMatrixIntegration {

    private static final String MATRIX_CLASS = "com.science.gtnl.common.machine.multiblock.AssemblerMatrix";
    private static final String DIRE_PATTERN_CLASS = "com.science.gtnl.utils.DireCraftingPatternDetails";
    private static final String ITEM_LIST_CLASS = "com.science.gtnl.utils.enums.GTNLItemList";
    private static final Api API = findApi();

    private AssemblerMatrixIntegration() {}

    public static boolean isAvailable() {
        return API != null;
    }

    public static ItemStack getIcon() {
        if (!isAvailable()) return null;
        try {
            Class<?> itemList = Class.forName(ITEM_LIST_CLASS);
            Object matrixItem = itemList.getField("AssemblerMatrix")
                .get(null);
            Object stack = itemList.getMethod("get", long.class, Object[].class)
                .invoke(matrixItem, 1L, new Object[0]);
            return stack instanceof ItemStack icon ? icon.copy() : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            AELog.error(e);
            return null;
        }
    }

    public static Result upload(IGrid grid, ItemStack pattern, World world) {
        if (API == null || grid == null
            || world == null
            || pattern == null
            || pattern.stackSize != 1
            || !(pattern.getItem() instanceof ItemEncodedPattern item)
            || pattern.hasTagCompound() && pattern.getTagCompound()
                .hasKey("tc_crafting")) return Result.REJECTED;

        try {
            ICraftingPatternDetails details = item.getPatternForItem(pattern.copy(), world);
            if (details == null || !details.isCraftable() && !API.direPatternType.isInstance(details)) {
                return Result.REJECTED;
            }
            IAEItemStack[] outputs = details.getCondensedOutputs();
            if (outputs == null || outputs.length == 0 || outputs[0] == null) return Result.REJECTED;

            List<Object> matrices = new ArrayList<>();
            for (IGridNode node : grid.getMachines(API.matrixType)) {
                Object matrix = node.getMachine();
                if (!API.matrixType.isInstance(matrix)) continue;
                matrices.add(matrix);
                Object possibleOutputs = API.getPossibleOutputs.invoke(matrix);
                if (!(possibleOutputs instanceof Collection<?> available)) return Result.REJECTED;
                if (available.contains(outputs[0])) return Result.DUPLICATE;
            }

            // Match GTNL's upload button: check every matrix for duplicates before inserting into the first free one.
            for (Object matrix : matrices) {
                Object inventory = API.getInventory.invoke(matrix);
                if (Boolean.TRUE.equals(API.insertPattern.invoke(inventory, pattern.copy()))) {
                    return Result.UPLOADED;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            AELog.error(e);
        }
        return Result.REJECTED;
    }

    private static Api findApi() {
        if (!Loader.isModLoaded("sciencenotleisure")) return null;
        try {
            ClassLoader loader = AssemblerMatrixIntegration.class.getClassLoader();
            Class<? extends IGridHost> matrixType = Class.forName(MATRIX_CLASS, false, loader)
                .asSubclass(IGridHost.class);
            Class<?> direPatternType = Class.forName(DIRE_PATTERN_CLASS, false, loader);
            Method getPossibleOutputs = matrixType.getMethod("getPossibleOutputs");
            Method getInventory = matrixType.getMethod("getInventory");
            Method insertPattern = getInventory.getReturnType()
                .getMethod("insertPattern", ItemStack.class);
            return new Api(matrixType, direPatternType, getPossibleOutputs, getInventory, insertPattern);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            AELog.error(e);
            return null;
        }
    }

    public enum Result {
        REJECTED,
        DUPLICATE,
        UPLOADED
    }

    private static final class Api {

        private final Class<? extends IGridHost> matrixType;
        private final Class<?> direPatternType;
        private final Method getPossibleOutputs;
        private final Method getInventory;
        private final Method insertPattern;

        private Api(Class<? extends IGridHost> matrixType, Class<?> direPatternType, Method getPossibleOutputs,
            Method getInventory, Method insertPattern) {
            this.matrixType = matrixType;
            this.direPatternType = direPatternType;
            this.getPossibleOutputs = getPossibleOutputs;
            this.getInventory = getInventory;
            this.insertPattern = insertPattern;
        }
    }
}

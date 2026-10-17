package com.asdflj.ae2thing.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.util.IInterfaceViewable;
import appeng.core.AELog;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.WrapperInventoryRange;

public final class InterfacePatternInventory {

    private static final String GTNL_SUPER_HATCH = "com.science.gtnl.common.machine.hatch.SuperCraftingInputHatchME";
    private static final String PROGRAMMABLE_PATTERN_PREFIX = "reobf.proghatches.gt.metatileentity.Pattern";
    private static final ClassValue<Optional<Field>> PATTERN_ARRAY = new ClassValue<>() {

        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (!GTNL_SUPER_HATCH.equals(current.getName())) continue;
                try {
                    return Optional.of(current.getField("internalInventory"));
                } catch (ReflectiveOperationException | SecurityException e) {
                    AELog.error(e);
                    break;
                }
            }
            return Optional.empty();
        }
    };
    private static final ClassValue<Optional<Method>> PROGRAMMABLE_REFRESH = new ClassValue<>() {

        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            if (!type.getName()
                .startsWith(PROGRAMMABLE_PATTERN_PREFIX)) return Optional.empty();
            return findMethod(type, "refresh");
        }
    };
    private static final ClassValue<Optional<Field>> PENDING_PATTERN_SYNC = new ClassValue<>() {

        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            if (PROGRAMMABLE_REFRESH.get(type)
                .isEmpty()
                && PATTERN_ARRAY.get(type)
                    .isEmpty())
                return Optional.empty();
            return findField(type, "needPatternSync", boolean.class);
        }
    };

    private InterfacePatternInventory() {}

    public static boolean hasExtendedCapacity(IInterfaceViewable host) {
        return getPatternCapacity(host) >= 0;
    }

    private static int getPatternCapacity(IInterfaceViewable host) {
        Optional<Field> field = PATTERN_ARRAY.get(host.getClass());
        if (field.isPresent()) {
            try {
                if (field.get()
                    .get(host) instanceof Object[]patterns) return patterns.length;
            } catch (IllegalAccessException e) {
                AELog.error(e);
            }
        }
        return -1;
    }

    public static int getSlotCount(IInterfaceViewable host) {
        int capacity = getPatternCapacity(host);
        return capacity < 0 ? host.numSlots()
            : Math.min(
                capacity,
                host.getPatterns()
                    .getSizeInventory());
    }

    public static int getRows(IInterfaceViewable host) {
        int capacity = getPatternCapacity(host);
        if (capacity < 0) return host.rows();
        int slots = Math.min(
            capacity,
            host.getPatterns()
                .getSizeInventory());
        int columns = Math.max(1, host.rowSize());
        return (slots + columns - 1) / columns;
    }

    public static IInventory getPatterns(IInterfaceViewable host) {
        IInventory inventory = host.getPatterns();
        int capacity = getPatternCapacity(host);
        if (capacity < 0) return inventory;
        return new WrapperInventoryRange(inventory, 0, Math.min(capacity, inventory.getSizeInventory()), true);
    }

    public static Batch beginBatch(IInterfaceViewable host) {
        if (!(host instanceof IInterfaceHost interfaceHost)) return null;
        DualityInterface duality = interfaceHost.getInterfaceDuality();
        IInventory patterns = host.getPatterns();
        if (duality == null || patterns != duality.getPatterns()
            || !(patterns instanceof AppEngInternalInventory inventory)) return null;
        inventory.setTileEntity(null);
        return new Batch(inventory, duality);
    }

    public static void endBatch(Batch batch, boolean changed) {
        if (batch == null) return;
        batch.inventory.setTileEntity(batch.duality);
        if (changed) {
            batch.inventory.markDirty();
            batch.duality.updateCraftingList();
        }
    }

    public static void notifyPatternChange(IInterfaceViewable host) {
        if (host instanceof IInterfaceHost) {
            return;
        }

        if (refreshProgrammablePatternCache(host)) {
            IGridNode node = host.getGridNode(ForgeDirection.UNKNOWN);
            if (node != null && node.isActive()) clearPendingPatternSync(host);
            return;
        }

        if (!(host instanceof ICraftingProvider provider)) return;
        IGridNode node = host.getGridNode(ForgeDirection.UNKNOWN);
        if (node == null || !node.isActive()) return;
        IGrid grid = node.getGrid();
        if (grid != null) {
            grid.postEvent(new MENetworkCraftingPatternChange(provider, node));
            clearPendingPatternSync(host);
        }
    }

    private static boolean refreshProgrammablePatternCache(IInterfaceViewable host) {
        Method refresh = PROGRAMMABLE_REFRESH.get(host.getClass())
            .orElse(null);
        if (refresh == null) return false;
        try {
            refresh.invoke(host);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            AELog.error(e);
            return false;
        }
    }

    private static void clearPendingPatternSync(IInterfaceViewable host) {
        Field field = PENDING_PATTERN_SYNC.get(host.getClass())
            .orElse(null);
        if (field == null) return;
        try {
            field.setBoolean(host, false);
        } catch (IllegalAccessException | RuntimeException e) {
            AELog.error(e);
        }
    }

    private static Optional<Field> findField(Class<?> type, String name, Class<?> fieldType) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                if (field.getType() != fieldType) return Optional.empty();
                field.setAccessible(true);
                return Optional.of(field);
            } catch (NoSuchFieldException ignored) {} catch (SecurityException e) {
                AELog.error(e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<Method> findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
                // The official method may live on a shared parent hatch.
            } catch (SecurityException e) {
                AELog.error(e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static final class Batch {

        private final AppEngInternalInventory inventory;
        private final DualityInterface duality;

        private Batch(AppEngInternalInventory inventory, DualityInterface duality) {
            this.inventory = inventory;
            this.duality = duality;
        }
    }

    public interface Tracker {

        NBTTagCompound ae2thing$updatePattern(int slot);

        PacketInterfaceTerminalUpdate ae2thing$syncPatternChanges(PacketInterfaceTerminalUpdate update);
    }
}

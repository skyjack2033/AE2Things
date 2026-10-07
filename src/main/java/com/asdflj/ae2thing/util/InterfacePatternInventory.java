package com.asdflj.ae2thing.util;

import java.lang.reflect.Field;
import java.util.Optional;

import net.minecraft.inventory.IInventory;

import appeng.api.util.IInterfaceViewable;
import appeng.core.AELog;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.util.inv.WrapperInventoryRange;

public final class InterfacePatternInventory {

    private static final String GTNL_SUPER_HATCH = "com.science.gtnl.common.machine.hatch.SuperCraftingInputHatchME";
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
        // GTNL exposes its entire hatch inventory, including circuit and manual input slots.
        return new WrapperInventoryRange(inventory, 0, Math.min(capacity, inventory.getSizeInventory()), false);
    }

    public interface Tracker {

        PacketInterfaceTerminalUpdate ae2thing$syncPatternChanges(PacketInterfaceTerminalUpdate update);
    }
}

package com.asdflj.ae2thing.util;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;

import appeng.api.util.IInterfaceViewable;

public final class InterfaceTerminalTarget {

    private static final ClassValue<Optional<TrackerFields>> TRACKER_FIELDS = new ClassValue<>() {

        @Override
        protected Optional<TrackerFields> computeValue(Class<?> type) {
            try {
                return Optional.of(new TrackerFields(type));
            } catch (ReflectiveOperationException | SecurityException e) {
                return Optional.empty();
            }
        }
    };

    private InterfaceTerminalTarget() {}

    /**
     * Resolve the actual host registered by AE2, including wrapper hosts and Programmable Hatches covers. The
     * tracker ID is signed; covers use its high bit. Coordinates and side must describe that same tracked entry.
     */
    @Nullable
    public static IInterfaceViewable resolve(Map<IInterfaceViewable, ?> tracked, NBTTagCompound tag) {
        return find(tracked, tag);
    }

    @Nullable
    public static Long getId(Map<IInterfaceViewable, ?> tracked, IInterfaceViewable host) {
        Object tracker = tracked.get(host);
        if (tracker == null) return null;
        Optional<TrackerFields> fields = TRACKER_FIELDS.get(tracker.getClass());
        if (fields.isEmpty()) return null;
        try {
            return fields.get().id.getLong(tracker);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    // Kept independent of concrete host implementations so target identity can be tested without a running world.
    @Nullable
    static <T> T find(Map<T, ?> tracked, NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("entryId", NBT.TAG_LONG)
            || !tag.hasKey("x", NBT.TAG_INT)
            || !tag.hasKey("y", NBT.TAG_INT)
            || !tag.hasKey("z", NBT.TAG_INT)
            || !tag.hasKey("dim", NBT.TAG_INT)
            || !tag.hasKey("side", NBT.TAG_INT)) return null;

        long id = tag.getLong("entryId");
        for (Map.Entry<T, ?> entry : tracked.entrySet()) {
            Object tracker = entry.getValue();
            if (tracker == null) continue;
            Optional<TrackerFields> fields = TRACKER_FIELDS.get(tracker.getClass());
            if (fields.isEmpty()) continue;
            try {
                if (fields.get()
                    .matches(tracker, id, tag)) return entry.getKey();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private static final class TrackerFields {

        private final Field id;
        private final Field x;
        private final Field y;
        private final Field z;
        private final Field dim;
        private final Field side;

        private TrackerFields(Class<?> type) throws NoSuchFieldException {
            this.id = field(type, "id", long.class);
            this.x = field(type, "x", int.class);
            this.y = field(type, "y", int.class);
            this.z = field(type, "z", int.class);
            this.dim = field(type, "dim", int.class);
            this.side = field(type, "side", Enum.class);
        }

        private boolean matches(Object tracker, long expectedId, NBTTagCompound tag) throws IllegalAccessException {
            if (this.id.getLong(tracker) != expectedId) return false;
            Object direction = this.side.get(tracker);
            return this.x.getInt(tracker) == tag.getInteger("x") && this.y.getInt(tracker) == tag.getInteger("y")
                && this.z.getInt(tracker) == tag.getInteger("z")
                && this.dim.getInt(tracker) == tag.getInteger("dim")
                && direction instanceof Enum<?>value
                && value.ordinal() == tag.getInteger("side");
        }

        private static Field field(Class<?> type, String name, Class<?> expectedType) throws NoSuchFieldException {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    if (!expectedType.isAssignableFrom(field.getType())) throw new NoSuchFieldException(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // A subclass may inherit the private tracker fields.
                }
            }
            throw new NoSuchFieldException(type.getName() + "." + name);
        }
    }
}

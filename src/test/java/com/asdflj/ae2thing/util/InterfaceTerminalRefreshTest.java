package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.util.IInterfaceViewable;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

public class InterfaceTerminalRefreshTest {

    @Test
    public void slowProvidersYieldBetweenHostsAndSendNamesBeforeTheRecipeMapSweepFinishes() {
        AtomicLong time = new AtomicLong();
        Map<IInterfaceViewable, Tracker> tracked = hosts(3);
        InterfaceTerminalRefresh refresh = new InterfaceTerminalRefresh(time::get, host -> {
            time.addAndGet(TimeUnit.MILLISECONDS.toNanos(3));
            return Set.of("assembler");
        });

        assertNotNull(refresh.tick(tracked));
        assertEquals(1, nameReads(tracked));
        assertNull(refresh.takeRecipeMaps());
        assertNotNull(refresh.tick(tracked));
        assertEquals(2, nameReads(tracked));
        assertNull(refresh.takeRecipeMaps());
        assertNotNull(refresh.tick(tracked));
        assertEquals(3, nameReads(tracked));
        assertEquals(
            3,
            refresh.takeRecipeMaps()
                .size());
    }

    @Test
    public void networkChangesDoNotRestartPendingWorkOrKeepRemovedTrackerIds() {
        Map<IInterfaceViewable, Tracker> tracked = hosts(40);
        List<IInterfaceViewable> original = new ArrayList<>(tracked.keySet());
        InterfaceTerminalRefresh refresh = new InterfaceTerminalRefresh(() -> 0, host -> Set.of("assembler"));
        refresh.tick(tracked);
        assertEquals(16, nameReads(tracked));
        assertNull(refresh.takeRecipeMaps());

        tracked.remove(original.get(0));
        Tracker removed = tracked.remove(original.get(19));
        tracked.put(original.get(20), new Tracker(10021));
        Tracker added = new Tracker(Long.MIN_VALUE);
        tracked.put(host(), added);
        refresh.invalidate();
        refresh.tick(tracked);
        refresh.invalidate();
        refresh.tick(tracked);

        Map<Long, Set<String>> first = refresh.takeRecipeMaps();
        assertNotNull(first);
        assertEquals(38, first.size());
        assertFalse(first.containsKey(1L));
        assertFalse(first.containsKey(20L));
        assertFalse(first.containsKey(21L));
        assertTrue(first.containsKey(10021L));
        assertEquals(0, removed.nameReads);

        for (int tick = 0; tick < 3; tick++) refresh.tick(tracked);
        Map<Long, Set<String>> second = refresh.takeRecipeMaps();
        assertEquals(tracked.size(), second.size());
        assertTrue(second.containsKey(Long.MIN_VALUE));
        assertEquals(1, added.nameReads);
        assertTrue(
            tracked.values()
                .stream()
                .allMatch(tracker -> tracker.nameReads > 0));
    }

    @Test
    public void unchangedMetadataIsNotResentAndAnEmptyNetworkClearsTheClientSnapshot() {
        Map<IInterfaceViewable, Tracker> tracked = hosts(1);
        InterfaceTerminalRefresh refresh = new InterfaceTerminalRefresh(() -> 0, host -> Set.of("assembler"));
        assertNotNull(refresh.tick(tracked));
        assertNotNull(refresh.takeRecipeMaps());
        for (int tick = 0; tick < 20; tick++) assertNull(refresh.tick(tracked));
        assertEquals(1, nameReads(tracked));
        assertNull(refresh.tick(tracked));
        assertEquals(2, nameReads(tracked));
        assertNull(refresh.takeRecipeMaps());

        tracked.clear();
        refresh.invalidate();
        assertNull(refresh.tick(tracked));
        assertTrue(
            refresh.takeRecipeMaps()
                .isEmpty());
        assertNull(refresh.takeRecipeMaps());
    }

    private static int nameReads(Map<IInterfaceViewable, Tracker> tracked) {
        return tracked.values()
            .stream()
            .mapToInt(tracker -> tracker.nameReads)
            .sum();
    }

    private static Map<IInterfaceViewable, Tracker> hosts(int count) {
        Map<IInterfaceViewable, Tracker> tracked = new LinkedHashMap<>();
        for (int i = 1; i <= count; i++) tracked.put(host(), new Tracker(i));
        return tracked;
    }

    private static IInterfaceViewable host() {
        return (IInterfaceViewable) Proxy.newProxyInstance(
            InterfaceTerminalRefreshTest.class.getClassLoader(),
            new Class<?>[] { IInterfaceViewable.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError(method.getName());
            });
    }

    private static final class Tracker implements InterfacePatternInventory.Tracker {

        private final long id;
        private int nameReads;

        private Tracker(long id) {
            this.id = id;
        }

        @Override
        public long ae2thing$getId() {
            return this.id;
        }

        @Override
        public PacketInterfaceTerminalUpdate ae2thing$syncName(PacketInterfaceTerminalUpdate update) {
            if (++this.nameReads == 1) {
                if (update == null) update = new PacketInterfaceTerminalUpdate();
                update.addRenamedEntry(this.id, "Assembler", null, null);
            }
            return update;
        }

        @Override
        public NBTTagCompound ae2thing$updatePattern(int slot) {
            throw new AssertionError("Unexpected pattern write");
        }

        @Override
        public PacketInterfaceTerminalUpdate ae2thing$syncPatternChanges(PacketInterfaceTerminalUpdate update) {
            return update;
        }
    }
}

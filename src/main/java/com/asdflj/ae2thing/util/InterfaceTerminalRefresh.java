package com.asdflj.ae2thing.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;

import appeng.api.util.IInterfaceViewable;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

public final class InterfaceTerminalRefresh {

    private static final int REFRESH_TICKS = 20;
    private static final int MAX_HOSTS_PER_TICK = 16;
    private static final long BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(2);

    private final LongSupplier clock;
    private final Function<IInterfaceViewable, Set<String>> resolver;
    private final Map<IInterfaceViewable, ResolvedMaps> resolved = new LinkedHashMap<>();
    private List<IInterfaceViewable> pending;
    private int cursor;
    private int cooldown;
    private boolean invalidated;
    private Map<Long, Set<String>> lastRecipeMaps;
    private Map<Long, Set<String>> changedRecipeMaps;

    public InterfaceTerminalRefresh() {
        this(System::nanoTime, InterfaceRecipeMaps::resolve);
    }

    InterfaceTerminalRefresh(LongSupplier clock, Function<IInterfaceViewable, Set<String>> resolver) {
        this.clock = clock;
        this.resolver = resolver;
    }

    public void invalidate() {
        this.invalidated = true;
    }

    public PacketInterfaceTerminalUpdate tick(Map<IInterfaceViewable, ?> tracked) {
        if (this.pending == null) {
            if (!this.invalidated && this.cooldown > 0) {
                this.cooldown--;
                return null;
            }
            this.pending = new ArrayList<>(tracked.keySet());
            this.cursor = 0;
            this.invalidated = false;
        }
        long started = this.clock.getAsLong();
        PacketInterfaceTerminalUpdate update = null;
        int processed = 0;
        while (this.cursor < this.pending.size() && processed < MAX_HOSTS_PER_TICK) {
            IInterfaceViewable host = this.pending.get(this.cursor++);
            if (tracked.get(host) instanceof InterfacePatternInventory.Tracker tracker) {
                update = tracker.ae2thing$syncName(update);
                update = tracker.ae2thing$syncPatternChanges(update);
                this.resolved.put(host, new ResolvedMaps(tracker.ae2thing$getId(), this.resolver.apply(host)));
            }
            processed++;
            if (this.clock.getAsLong() - started >= BUDGET_NANOS) break;
        }
        if (this.cursor == this.pending.size()) {
            this.resolved.keySet()
                .retainAll(tracked.keySet());
            Map<Long, Set<String>> snapshot = new LinkedHashMap<>();
            this.resolved.forEach((host, maps) -> {
                if (tracked.get(host) instanceof InterfacePatternInventory.Tracker tracker
                    && tracker.ae2thing$getId() == maps.id) snapshot.put(maps.id, maps.maps);
            });
            if (!snapshot.equals(this.lastRecipeMaps)) {
                this.lastRecipeMaps = snapshot;
                this.changedRecipeMaps = snapshot;
            }
            this.pending = null;
            this.cooldown = REFRESH_TICKS;
        }
        return update;
    }

    public Map<Long, Set<String>> takeRecipeMaps() {
        Map<Long, Set<String>> changed = this.changedRecipeMaps;
        this.changedRecipeMaps = null;
        return changed;
    }

    private record ResolvedMaps(long id, Set<String> maps) {}
}

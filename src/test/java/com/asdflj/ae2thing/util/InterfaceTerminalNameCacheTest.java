package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import appeng.api.util.IInterfaceViewable;
import appeng.helpers.ICustomNameObject;

public class InterfaceTerminalNameCacheTest {

    private static final String SELF_KEY = "gt.blockmachines.pattern.hatch";
    private static final String SELF_NAME = "Pattern Hatch";
    private static final String MACHINE = "gt.blockmachines.multimachine.assemblyline";

    @Test
    public void reusesTheMachineNameAcrossTerminalReopensAndStartsTheFallbackDeadlineOnDemand() {
        InterfaceTerminalNameCache cache = new InterfaceTerminalNameCache();
        Object host = new Object();
        assertEquals(MACHINE, resolve(cache, host, MACHINE, false, 0));
        assertEquals(MACHINE, resolve(cache, host, SELF_KEY + ".name", false, 60));
        assertEquals(MACHINE, resolve(cache, host, SELF_NAME, false, 64));
        assertEquals(SELF_NAME, resolve(cache, host, SELF_NAME, false, 65));
    }

    @Test
    public void legacyFallbackKeepsTheCurrentCircuitItemAndFluidSuffixes() {
        InterfaceTerminalNameCache cache = new InterfaceTerminalNameCache();
        Object host = new Object();
        assertEquals("Assembly Line [UV] -4 -Mold@2", resolve(cache, host, "Assembly Line [UV] -4 -Mold@2", true, 0));
        assertEquals(
            "Assembly Line [UV] -12 -New Mold@3 -Water",
            resolve(cache, host, SELF_NAME + " -12 -New Mold@3 -Water", true, 1));
        assertEquals("Assembly Line [UV] -7", resolve(cache, host, SELF_NAME + " -7", true, 2));
    }

    @Test
    public void aNewRealMachineNameImmediatelyReplacesTheOldNameAndResetsTheFallbackDeadline() {
        InterfaceTerminalNameCache cache = new InterfaceTerminalNameCache();
        Object host = new Object();
        resolve(cache, host, MACHINE, false, 0);
        resolve(cache, host, SELF_NAME, false, 1);
        assertEquals("Other Machine", resolve(cache, host, "Other Machine", false, 5));
        assertEquals("Other Machine", resolve(cache, host, SELF_NAME, false, 6));
        assertEquals("Other Machine", resolve(cache, host, SELF_NAME, false, 10));
        assertEquals(SELF_NAME, resolve(cache, host, SELF_NAME, false, 11));
    }

    @Test
    public void equalHostsNeverShareTheirCachedNames() {
        InterfaceTerminalNameCache cache = new InterfaceTerminalNameCache();
        Object first = new EqualHost();
        Object second = new EqualHost();
        resolve(cache, first, MACHINE, false, 0);
        assertEquals(SELF_NAME, resolve(cache, second, SELF_NAME, false, 1));
        assertEquals(MACHINE, resolve(cache, first, SELF_NAME, false, 1));
    }

    @Test
    public void selfNamePrefixesDoNotReplaceAnActualMachineName() {
        InterfaceTerminalNameCache cache = new InterfaceTerminalNameCache();
        Object host = new Object();
        resolve(cache, host, MACHINE, false, 0);
        assertEquals("Pattern Hatch Array", resolve(cache, host, "Pattern Hatch Array", true, 1));
        assertEquals("Pattern Hatch Array - 4", resolve(cache, host, SELF_NAME + " - 4", true, 2));
    }

    @Test
    public void customNamesTakePrecedenceBeforeTheAutomaticNameIsRead() {
        IInterfaceViewable host = (IInterfaceViewable) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { IInterfaceViewable.class, ICustomNameObject.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "hasCustomName" -> true;
            case "getCustomName" -> "My Machine - 8";
            default -> throw new AssertionError("Unexpected method: " + method.getName());
            });
        assertEquals("My Machine - 8", InterfaceTerminalNameCache.getRawName(host));
    }

    private static String resolve(InterfaceTerminalNameCache cache, Object host, String rawName, boolean legacy,
        long seconds) {
        return cache.resolve(host, rawName, SELF_NAME, SELF_KEY, legacy, TimeUnit.SECONDS.toNanos(seconds));
    }

    private static final class EqualHost {

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualHost;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}

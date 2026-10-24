package com.asdflj.ae2thing.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class ProgrammableHatchInventoryAdaptorTest {

    @Test
    public void recognizesBufferedPatternAndOcHatchesByTheirRealParent() {
        FixtureLoader loader = new FixtureLoader();
        String prefix = "reobf.proghatches.gt.metatileentity.";
        Class<?> base = loader.define(prefix + "DualInputHatch", Object.class);
        Class<?> buffered = loader.define(prefix + "BufferedDualInputHatch", base);
        Class<?> pattern = loader.define(prefix + "PatternDualInputHatch", buffered);
        Class<?> instance = loader.define(prefix + "PatternDualInputHatch$Inst", pattern);
        Class<?> oc = loader.define(prefix + "DualInputHachOC", base);

        for (Class<?> type : new Class<?>[] { base, buffered, pattern, instance, oc }) {
            assertTrue(ProgrammableHatchInventoryAdaptor.isSupportedHatch(type));
        }
    }

    @Test
    public void rejectsSimilarlyNamedSlavesWithoutTheSharedFluidBridge() {
        FixtureLoader loader = new FixtureLoader();
        String prefix = "reobf.proghatches.gt.metatileentity.";
        for (String name : new String[] { "DualInputHatchSlave", "DualInputHatchSlaveBus", "DualInputHatchSlaveHatch",
            "DualInputHatchInventoryMappingSlave" }) {
            assertFalse(ProgrammableHatchInventoryAdaptor.isSupportedHatch(loader.define(prefix + name, Object.class)));
        }
        assertFalse(ProgrammableHatchInventoryAdaptor.isSupportedHatch(Object.class));
    }

    @Test
    public void refusesAPartialPacketWithoutChangingTheHatch() {
        CapacityHandler handler = new CapacityHandler(400);
        for (int attempt = 0; attempt < 2; attempt++) {
            assertEquals(0, fill(handler, 1000, true, true));
        }
        assertEquals(0, handler.stored);
        assertEquals(0, handler.realFills);
        assertEquals(2, handler.simulations);
    }

    @Test
    public void acceptsTheEntirePacketOnceCapacityIsAvailable() {
        CapacityHandler handler = new CapacityHandler(1000);
        assertEquals(1000, fill(handler, 1000, true, true));
        assertEquals(1000, handler.stored);
        assertEquals(1, handler.realFills);
        assertEquals(1, handler.simulations);
    }

    @Test
    public void packetSimulationNeverModifiesTheHatch() {
        CapacityHandler handler = new CapacityHandler(1000);
        assertEquals(1000, fill(handler, 1000, true, false));
        assertEquals(0, handler.stored);
        assertEquals(0, handler.realFills);
        assertEquals(1, handler.simulations);
    }

    @Test
    public void nativeAndDropQuantitiesStillAllowPartialMillibucketTransfers() {
        CapacityHandler handler = new CapacityHandler(400);
        assertEquals(400, fill(handler, 1000, false, false));
        assertEquals(0, handler.stored);
        assertEquals(400, fill(handler, 1000, false, true));
        assertEquals(400, handler.stored);
        assertEquals(1, handler.realFills);
        assertEquals(1, handler.simulations);
    }

    private static int fill(CapacityHandler handler, int amount, boolean wholePacket, boolean modulate) {
        return ProgrammableHatchInventoryAdaptor
            .fillFluid(amount, wholePacket, modulate, doFill -> handler.fill(amount, doFill));
    }

    private static final class FixtureLoader extends ClassLoader {

        private Class<?> define(String name, Class<?> parent) {
            ClassWriter writer = new ClassWriter(0);
            writer.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                name.replace('.', '/'),
                null,
                parent.getName()
                    .replace('.', '/'),
                null);
            writer.visitEnd();
            byte[] bytes = writer.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static final class CapacityHandler {

        private final int capacity;
        private int stored;
        private int simulations;
        private int realFills;

        private CapacityHandler(int capacity) {
            this.capacity = capacity;
        }

        private int fill(int amount, boolean doFill) {
            int accepted = Math.min(capacity - stored, amount);
            if (doFill) {
                realFills++;
                stored += accepted;
            } else {
                simulations++;
            }
            return accepted;
        }
    }
}

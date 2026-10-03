package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class InterfaceTerminalTargetTest {

    @Test
    public void nativeTrackerProvidesTheIdentityFieldsRequiredByTheResolver() throws IOException {
        String resource = "appeng/container/implementations/ContainerInterfaceTerminal$InvTracker.class";
        InputStream input = getClass().getClassLoader()
            .getResourceAsStream(resource);
        assertNotNull("Missing pinned AE2 tracker class", input);
        Map<String, String> fields = new LinkedHashMap<>();
        try (InputStream bytecode = input) {
            new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                    Object value) {
                    fields.put(name, descriptor);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals("J", fields.get("id"));
        for (String field : new String[] { "x", "y", "z", "dim" }) {
            assertEquals("Unexpected native tracker field type: " + field, "I", fields.get(field));
        }
        assertEquals("Lnet/minecraftforge/common/util/ForgeDirection;", fields.get("side"));
    }

    @Test
    public void resolvesSignedCoverIdsAndReturnsTheExactTrackedWrapper() {
        Object firstWrapper = new Object();
        Object secondWrapper = new Object();
        Map<Object, Object> tracked = new LinkedHashMap<>();
        tracked.put(firstWrapper, new Tracker(4));
        tracked.put(secondWrapper, new Tracker(Long.MIN_VALUE | 4));

        assertSame(firstWrapper, InterfaceTerminalTarget.find(tracked, target(4)));
        assertSame(secondWrapper, InterfaceTerminalTarget.find(tracked, target(Long.MIN_VALUE | 4)));
    }

    @Test
    public void neverFallsBackToAnotherHostAtTheSameCoordinates() {
        Map<Object, Object> tracked = new LinkedHashMap<>();
        tracked.put(new Object(), new Tracker(0));
        tracked.put(new Object(), new Tracker(1));

        assertNull(InterfaceTerminalTarget.find(tracked, target(2)));
        NBTTagCompound missingId = target(0);
        missingId.removeTag("entryId");
        assertNull(InterfaceTerminalTarget.find(tracked, missingId));
        NBTTagCompound wrongIdType = target(0);
        wrongIdType.setInteger("entryId", 0);
        assertNull(InterfaceTerminalTarget.find(tracked, wrongIdType));
    }

    @Test
    public void rejectsStaleOrMismatchedCoordinatesDimensionAndSide() {
        Map<Object, Object> tracked = Map.of(new Object(), new Tracker(7));

        for (String key : new String[] { "x", "y", "z", "dim", "side" }) {
            NBTTagCompound wrong = target(7);
            wrong.setInteger(key, wrong.getInteger(key) + 1);
            assertNull("Unexpected match for changed " + key, InterfaceTerminalTarget.find(tracked, wrong));

            NBTTagCompound missing = target(7);
            missing.removeTag(key);
            assertNull("Unexpected match for missing " + key, InterfaceTerminalTarget.find(tracked, missing));
        }
    }

    @Test
    public void removedEntriesAndUnsupportedTrackersCannotResolve() {
        Object wrapper = new Object();
        Map<Object, Object> tracked = new LinkedHashMap<>();
        tracked.put(new Object(), new Object());
        tracked.put(wrapper, new Tracker(9));
        assertSame(wrapper, InterfaceTerminalTarget.find(tracked, target(9)));

        tracked.remove(wrapper);
        assertNull(InterfaceTerminalTarget.find(tracked, target(9)));
        assertNull(InterfaceTerminalTarget.find(tracked, null));
    }

    private static NBTTagCompound target(long id) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("entryId", id);
        tag.setInteger("x", 12);
        tag.setInteger("y", 64);
        tag.setInteger("z", -23);
        tag.setInteger("dim", -1);
        tag.setInteger("side", Direction.WEST.ordinal());
        return tag;
    }

    private enum Direction {
        DOWN,
        UP,
        NORTH,
        SOUTH,
        WEST,
        EAST,
        UNKNOWN
    }

    private static final class Tracker {

        private final long id;
        private final int x = 12;
        private final int y = 64;
        private final int z = -23;
        private final int dim = -1;
        private final Direction side = Direction.WEST;

        private Tracker(long id) {
            this.id = id;
        }
    }
}

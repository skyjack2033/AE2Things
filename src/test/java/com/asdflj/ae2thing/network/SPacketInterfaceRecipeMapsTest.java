package com.asdflj.ae2thing.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

public class SPacketInterfaceRecipeMapsTest {

    @Test
    public void roundTripsWindowSignedIdsAndEmptyMappings() {
        SPacketInterfaceRecipeMaps message = new SPacketInterfaceRecipeMaps(
            7,
            Map.of(Long.MIN_VALUE | 42, Set.of("gt.recipe.assembler", "gt.recipe.macerator"), 5L, Set.of()));
        ByteBuf encoded = Unpooled.buffer();
        message.toBytes(encoded);
        ByteBuf before = encoded.copy();
        SPacketInterfaceRecipeMaps decoded = new SPacketInterfaceRecipeMaps();
        decoded.fromBytes(encoded);
        ByteBuf after = Unpooled.buffer();
        decoded.toBytes(after);
        assertEquals(before, after);
        assertFalse(encoded.isReadable());
    }

    @Test
    public void sendsAnExplicitEmptySnapshot() {
        ByteBuf encoded = Unpooled.buffer();
        new SPacketInterfaceRecipeMaps(12, Map.of()).toBytes(encoded);
        assertEquals(12, encoded.readInt());
        assertEquals(0, encoded.readInt());
        assertFalse(encoded.isReadable());
    }

    @Test(expected = DecoderException.class)
    public void rejectsAnUnboundedInterfaceCount() {
        ByteBuf encoded = Unpooled.buffer();
        encoded.writeInt(12);
        encoded.writeInt(Integer.MAX_VALUE);
        new SPacketInterfaceRecipeMaps().fromBytes(encoded);
    }
}

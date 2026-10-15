package com.asdflj.ae2thing.network;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

public class CPacketPullBookmarksTest {

    @Test
    public void emptyRequestRoundTripKeepsWindowId() {
        ByteBuf encoded = Unpooled.buffer();
        ByteBuf roundTrip = Unpooled.buffer();
        try {
            new CPacketPullBookmarks(42, Collections.emptyList()).toBytes(encoded);
            CPacketPullBookmarks decoded = new CPacketPullBookmarks();
            decoded.fromBytes(encoded);
            decoded.toBytes(roundTrip);
            assertEquals(42, roundTrip.readInt());
            assertEquals(0, roundTrip.readInt());
        } finally {
            encoded.release();
            roundTrip.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsOversizedPacketBeforeDecompression() {
        ByteBuf encoded = Unpooled.buffer(CPacketPullBookmarks.MAX_PACKET_BYTES + 13);
        try {
            encoded.writerIndex(encoded.capacity());
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsTooManyBookmarksBeforeDecompression() {
        ByteBuf encoded = Unpooled.buffer();
        try {
            encoded.writeInt(42);
            encoded.writeInt(CPacketPullBookmarks.MAX_BOOKMARKS + 1);
            encoded.writeInt(1);
            encoded.writeByte(0);
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsNegativeBookmarkCount() {
        ByteBuf encoded = Unpooled.buffer();
        try {
            encoded.writeInt(42);
            encoded.writeInt(-1);
            encoded.writeInt(1);
            encoded.writeByte(0);
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsMismatchedBookmarkCount() throws Exception {
        ByteBuf encoded = packet(1, new NBTTagList());
        try {
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsOversizedAmountBeforeResolvingItem() throws Exception {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("amount", CPacketPullBookmarks.MAX_REQUESTED_ITEMS + 1);
        NBTTagList bookmarks = new NBTTagList();
        bookmarks.appendTag(entry);
        ByteBuf encoded = packet(1, bookmarks);
        try {
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsZeroAmountBeforeResolvingItem() throws Exception {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("amount", 0);
        NBTTagList bookmarks = new NBTTagList();
        bookmarks.appendTag(entry);
        ByteBuf encoded = packet(1, bookmarks);
        try {
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsTrailingBytes() throws Exception {
        ByteBuf encoded = packet(0, new NBTTagList());
        try {
            encoded.writeByte(0);
            new CPacketPullBookmarks().fromBytes(encoded);
        } finally {
            encoded.release();
        }
    }

    private static ByteBuf packet(int count, NBTTagList bookmarks) throws Exception {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setTag("items", bookmarks);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(payload, compressed);
        byte[] bytes = compressed.toByteArray();
        ByteBuf encoded = Unpooled.buffer();
        encoded.writeInt(42);
        encoded.writeInt(count);
        encoded.writeInt(bytes.length);
        encoded.writeBytes(bytes);
        return encoded;
    }
}

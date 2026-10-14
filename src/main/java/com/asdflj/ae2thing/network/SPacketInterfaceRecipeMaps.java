package com.asdflj.ae2thing.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;

import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

/** A complete snapshot; an empty snapshot also clears metadata from the previous terminal update. */
public class SPacketInterfaceRecipeMaps implements IMessage {

    private static final int MAX_INTERFACES = 65_536;
    private static final int MAX_MAPS_PER_INTERFACE = 256;
    private static final int MAX_MAP_NAME_LENGTH = 512;

    private int windowId;
    private Map<Long, Set<String>> recipeMaps;

    public SPacketInterfaceRecipeMaps() {}

    public SPacketInterfaceRecipeMaps(int windowId, Map<Long, Set<String>> recipeMaps) {
        this.windowId = windowId;
        this.recipeMaps = new LinkedHashMap<>();
        for (Map.Entry<Long, Set<String>> entry : recipeMaps.entrySet()) {
            this.recipeMaps.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        int entries = readCount(buf, MAX_INTERFACES, Long.BYTES + Integer.BYTES, "interfaces");
        Map<Long, Set<String>> snapshot = new LinkedHashMap<>();
        for (int i = 0; i < entries; i++) {
            long id = buf.readLong();
            int count = readCount(buf, MAX_MAPS_PER_INTERFACE, Integer.BYTES, "recipe maps");
            Set<String> maps = new LinkedHashSet<>();
            for (int j = 0; j < count; j++) {
                maps.add(PacketDecodeUtil.readUtf16(buf, MAX_MAP_NAME_LENGTH, "recipe map name"));
            }
            snapshot.put(id, Collections.unmodifiableSet(maps));
        }
        this.recipeMaps = Collections.unmodifiableMap(snapshot);
    }

    private static int readCount(ByteBuf buf, int maximum, int entryBytes, String name) {
        int count = buf.readInt();
        if (count < 0 || count > maximum || count > buf.readableBytes() / entryBytes) {
            throw new DecoderException("Invalid " + name + " count: " + count);
        }
        return count;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeInt(this.recipeMaps.size());
        for (Map.Entry<Long, Set<String>> entry : this.recipeMaps.entrySet()) {
            buf.writeLong(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (String map : entry.getValue()) {
                buf.writeInt(map.length());
                for (int i = 0; i < map.length(); i++) {
                    buf.writeChar(map.charAt(i));
                }
            }
        }
    }

    public static class Handler implements IMessageHandler<SPacketInterfaceRecipeMaps, IMessage> {

        @Override
        public IMessage onMessage(SPacketInterfaceRecipeMaps message, MessageContext ctx) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.thePlayer != null
                && minecraft.thePlayer.openContainer instanceof ContainerWirelessDualInterfaceTerminal container
                && container.windowId == message.windowId) {
                container.updateInterfaceRecipeMaps(message.windowId, message.recipeMaps);
            }
            return null;
        }
    }
}

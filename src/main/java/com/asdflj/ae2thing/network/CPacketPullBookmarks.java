package com.asdflj.ae2thing.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.util.PatternStackCodec;

import appeng.api.config.Actionable;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public final class CPacketPullBookmarks implements IMessage {

    static final int MAX_BOOKMARKS = 128;
    static final int MAX_REQUESTED_ITEMS = 1_000_000;
    static final int MAX_PACKET_BYTES = 256 * 1024;
    private static final int MAX_UNCOMPRESSED_BYTES = 1024 * 1024;
    private static final int MAX_TRANSFER_BATCHES = 256;
    private static final int HEADER_BYTES = Integer.BYTES * 3;

    private int windowId;
    private List<ItemStack> items = new ArrayList<>();

    public CPacketPullBookmarks() {}

    public CPacketPullBookmarks(int windowId, List<ItemStack> bookmarks) {
        this.windowId = windowId;
        int remaining = MAX_REQUESTED_ITEMS;
        for (ItemStack bookmark : bookmarks) {
            if (bookmark == null || bookmark.stackSize <= 0 || PatternStackCodec.isFluidMarker(bookmark)) continue;
            ItemStack request = bookmark.copy();
            request.stackSize = Math.min(request.stackSize, remaining);
            this.items.add(request);
            remaining -= request.stackSize;
            if (remaining == 0 || this.items.size() == MAX_BOOKMARKS) break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound payload = new NBTTagCompound();
        NBTTagList bookmarks = new NBTTagList();
        for (ItemStack item : this.items) {
            NBTTagCompound entry = new NBTTagCompound();
            ItemStack identity = item.copy();
            identity.stackSize = 1;
            identity.writeToNBT(entry);
            entry.setInteger("amount", item.stackSize);
            bookmarks.appendTag(entry);
        }
        payload.setTag("items", bookmarks);
        try {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(payload, compressed);
            byte[] bytes = compressed.toByteArray();
            if (bytes.length > MAX_PACKET_BYTES) throw new EncoderException("Bookmark request is too large");
            buf.writeInt(this.windowId);
            buf.writeInt(this.items.size());
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        } catch (IOException e) {
            throw new EncoderException("Failed to encode bookmark request", e);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() < HEADER_BYTES || buf.readableBytes() > HEADER_BYTES + MAX_PACKET_BYTES) {
            throw new DecoderException("Invalid bookmark request size");
        }
        this.windowId = buf.readInt();
        int count = buf.readInt();
        int length = buf.readInt();
        if (count < 0 || count > MAX_BOOKMARKS
            || length <= 0
            || length > MAX_PACKET_BYTES
            || length != buf.readableBytes()) {
            throw new DecoderException("Invalid bookmark request header");
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        try {
            NBTTagCompound payload = CompressedStreamTools
                .func_152457_a(bytes, new NBTSizeTracker(MAX_UNCOMPRESSED_BYTES));
            NBTTagList bookmarks = payload.getTagList("items", 10);
            if (bookmarks.tagCount() != count) throw new DecoderException("Invalid bookmark request count");
            this.items = new ArrayList<>(count);
            int total = 0;
            for (int i = 0; i < count; i++) {
                NBTTagCompound entry = bookmarks.getCompoundTagAt(i);
                int amount = entry.getInteger("amount");
                if (amount <= 0 || amount > MAX_REQUESTED_ITEMS - total) {
                    throw new DecoderException("Invalid bookmark item amount");
                }
                ItemStack item = ItemStack.loadItemStackFromNBT(entry);
                if (item == null || PatternStackCodec.isFluidMarker(item)) {
                    throw new DecoderException("Invalid bookmark item");
                }
                item.stackSize = amount;
                this.items.add(item);
                total += amount;
            }
        } catch (IOException e) {
            throw new DecoderException("Failed to decode bookmark request", e);
        }
    }

    public static final class Handler implements IMessageHandler<CPacketPullBookmarks, IMessage> {

        @Override
        public IMessage onMessage(CPacketPullBookmarks message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player.openContainer instanceof ContainerWirelessDualInterfaceTerminal terminal
                && terminal.windowId == message.windowId
                && terminal.canPullBookmarks(player)) {
                pullItems(terminal, player, message.items);
                player.inventory.markDirty();
                terminal.detectAndSendChanges();
            }
            return null;
        }
    }

    private static void pullItems(ContainerWirelessDualInterfaceTerminal terminal, EntityPlayerMP player,
        List<ItemStack> requests) {
        IMEMonitor<IAEItemStack> monitor = terminal.getMonitor();
        InventoryAdaptor inventory = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
        if (inventory == null) return;
        int batches = 0;
        for (ItemStack requested : requests) {
            int remaining = requested.stackSize;
            while (remaining > 0 && terminal.canPullBookmarks(player)) {
                if (batches++ == MAX_TRANSFER_BATCHES) return;
                ItemStack batch = requested.copy();
                batch.stackSize = Math.min(remaining, Math.max(1, batch.getMaxStackSize()));
                IAEItemStack request = AEItemStack.create(batch);
                IAEItemStack available = monitor.extractItems(request, Actionable.SIMULATE, terminal.getActionSource());
                if (available == null || available.getStackSize() <= 0) break;
                ItemStack rejected = inventory.simulateAdd(available.getItemStack());
                long fitting = available.getStackSize() - (rejected == null ? 0 : rejected.stackSize);
                if (fitting <= 0) break;
                request.setStackSize(fitting);
                IAEItemStack extracted = Platform
                    .poweredExtraction(terminal.getPowerSource(), monitor, request, terminal.getActionSource());
                if (extracted == null || extracted.getStackSize() <= 0) break;
                remaining -= (int) extracted.getStackSize();
                ItemStack leftover = inventory.addItems(extracted.getItemStack());
                if (leftover != null && leftover.stackSize > 0) {
                    // A modded inventory may accept less than its simulation promised. Never discard that remainder.
                    IAEItemStack returned = monitor
                        .injectItems(AEItemStack.create(leftover), Actionable.MODULATE, terminal.getActionSource());
                    if (returned != null && returned.getStackSize() > 0) {
                        player.entityDropItem(returned.getItemStack(), 0);
                    }
                    break;
                }
            }
        }
    }
}

package com.asdflj.ae2thing.network;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.util.BlockPos;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.helpers.InventoryAction;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class CPacketInventoryAction implements IMessage {

    static IAEStack<?> normalizeCraftingStack(IAEStack<?> stack) {
        return stack;
    }

    public static void openCraftAmount(AEBaseContainer baseContainer, EntityPlayerMP sender, IAEStack<?> requestedStack) {
        final ContainerOpenContext context = baseContainer.getOpenContext();
        if (context == null) return;

        Object target = baseContainer.getTarget();
        final TileEntity te = context.getTile();
        if (te == null && !(target instanceof WirelessTerminal)) return;

        IAEStack<?> stack = requestedStack != null ? requestedStack : baseContainer.getTargetStack();
        stack = normalizeCraftingStack(stack);
        if (stack == null) return;

        baseContainer.setTargetStack(stack);
        if (te != null) {
            InventoryHandler.openGui(
                sender,
                te.getWorldObj(),
                new BlockPos(te),
                Objects.requireNonNull(context.getSide()),
                GuiType.CRAFTING_AMOUNT);
        } else {
            InventoryHandler.openGui(
                sender,
                sender.getEntityWorld(),
                new BlockPos(((WirelessTerminal) target).getInventorySlot(), 0, 0),
                Objects.requireNonNull(context.getSide()),
                GuiType.CRAFTING_AMOUNT_ITEM);
        }

        if (sender.openContainer instanceof final ContainerCraftAmount cca) {
            cca.setItemToCraft(baseContainer.getTargetStack());
            cca.detectAndSendChanges();
        }
    }

    private InventoryAction action;
    private int slot;
    private long id;
    private IAEStack<?> stack;
    private boolean isEmpty;

    public CPacketInventoryAction() {}

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = null;
        this.isEmpty = true;
    }

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id, IAEStack<?> stack) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = stack;
        this.isEmpty = stack == null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(id);
        buf.writeBoolean(isEmpty);
        if (!isEmpty) {
            try {
                IAEStack.writeToPacketGeneric(buf, stack);
            } catch (IOException e) {
                throw new EncoderException("Failed to encode inventory action stack", e);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = PacketDecodeUtil.readIntEnum(buf, InventoryAction.values(), "inventory action");
        slot = buf.readInt();
        id = buf.readLong();
        isEmpty = buf.readBoolean();
        if (!isEmpty) {
            try {
                stack = IAEStack.fromPacketGeneric(buf);
            } catch (IOException e) {
                throw new DecoderException("Failed to decode inventory action stack", e);
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryAction, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryAction message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender.openContainer instanceof final AEBaseContainer baseContainer) {
                if (message.action == InventoryAction.AUTO_CRAFT) {
                    openCraftAmount(baseContainer, sender, message.stack);
                } else {
                    baseContainer.doAction(sender, message.action, message.slot, message.id);
                }
            }
            return null;
        }
    }
}

package com.asdflj.ae2thing.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.network.CPacketPullBookmarks;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.PatternStackCodec;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import codechicken.nei.api.IBookmarkContainerHandler;

public final class DualInterfaceBookmarkContainerHandler implements IBookmarkContainerHandler {

    public static final DualInterfaceBookmarkContainerHandler INSTANCE = new DualInterfaceBookmarkContainerHandler();

    private DualInterfaceBookmarkContainerHandler() {}

    @Override
    public List<ItemStack> getStorageStacks(GuiContainer gui) {
        List<ItemStack> stacks = new ArrayList<>();
        if (gui instanceof GuiWirelessDualInterfaceTerminal terminal) {
            for (IAEStack<?> stack : Ae2ReflectClient.getList(terminal.getRepo())) {
                if (stack instanceof IAEItemStack item && item.getStackSize() > 0) {
                    ItemStack stored = item.getItemStack();
                    if (!PatternStackCodec.isFluidMarker(stored)) stacks.add(stored.copy());
                }
            }
        }
        return stacks;
    }

    @Override
    public void pullBookmarkItemsFromContainer(GuiContainer gui, ArrayList<ItemStack> items) {
        if (gui instanceof GuiWirelessDualInterfaceTerminal terminal && items != null && !items.isEmpty()) {
            AE2Thing.proxy.netHandler.sendToServer(new CPacketPullBookmarks(terminal.inventorySlots.windowId, items));
        }
    }
}

package com.asdflj.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.ae2thing.loader.ItemAndBlockHolder;
import com.asdflj.ae2thing.util.Ae2ReflectClient;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiTabButton;

public class GuiCraftingStatus extends appeng.client.gui.implementations.GuiCraftingStatus {

    private final ITerminalHost host;

    public GuiCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
        host = te;
    }

    @Override
    public void initGui() {
        if (host instanceof PartInfusionPatternTerminal) {
            Ae2ReflectClient.rewriteIcon(this, ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack());
        } else if (host instanceof WirelessDualInterfaceTerminalInventory) {
            Ae2ReflectClient.rewriteIcon(this, ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack());
        }
        super.initGui();
        if (originalGuiBtn == null) initPrimaryGuiButton();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == originalGuiBtn) {
            if (host instanceof PartInfusionPatternTerminal) {
                InventoryHandler.switchGui(GuiType.INFUSION_PATTERN_TERMINAL);
            } else if (host instanceof WirelessDualInterfaceTerminalInventory) {
                InventoryHandler.switchGui(GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL);
            }
        } else {
            super.actionPerformed(btn);
        }
    }

    @Override
    public void initPrimaryGuiButton() {
        if (originalGuiBtn != null && this.buttonList.contains(originalGuiBtn)) return;

        final ItemStack icon;
        if (host instanceof PartInfusionPatternTerminal) {
            icon = ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack();
        } else if (host instanceof WirelessDualInterfaceTerminalInventory) {
            icon = ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
        } else {
            super.initPrimaryGuiButton();
            originalGuiBtn = Ae2ReflectClient.getOriginalGuiButton(this);
            return;
        }

        this.buttonList.add(
            originalGuiBtn = new GuiTabButton(
                this.guiLeft + this.xSize - 25,
                this.guiTop - 4,
                icon,
                icon.getDisplayName(),
                itemRender));
        originalGuiBtn.setHideEdge(13);
    }
}

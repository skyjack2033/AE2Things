package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.ae2thing.loader.ItemAndBlockHolder;
import com.asdflj.ae2thing.util.BlockPos;

import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;

public class ContainerCraftConfirm extends appeng.container.implementations.ContainerCraftConfirm {

    public ContainerCraftConfirm(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }

    @Override
    public void setOpenContext(ContainerOpenContext context) {
        super.setOpenContext(context);
        if (context == null || context.getSide() == null) return;

        final GuiType originalGui;
        final ItemStack icon;
        final BlockPos pos;
        final TileEntity tile = context.getTile();
        final IActionHost ah = this.getActionHost();
        if (ah instanceof PartInfusionPatternTerminal) {
            if (tile == null) return;
            originalGui = GuiType.INFUSION_PATTERN_TERMINAL;
            icon = ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack();
            pos = new BlockPos(tile);
        } else if (ah instanceof WirelessDualInterfaceTerminalInventory terminal) {
            originalGui = GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
            icon = ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
            pos = new BlockPos(terminal.getInventorySlot(), 0, 0);
        } else {
            return;
        }

        final World world = this.getWorld();
        final ForgeDirection face = context.getSide();
        this.setPrimaryGui(new PrimaryGui(originalGui, icon, tile, face) {

            @Override
            public void open(EntityPlayer player) {
                InventoryHandler.openGui(player, world, pos, face, originalGui);
            }
        });
    }
}

package com.asdflj.ae2thing.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import com.asdflj.ae2thing.client.textures.BlockTexture;
import com.asdflj.ae2thing.common.tile.TileExIOPort;

import appeng.block.storage.BlockIOPort;
import appeng.client.render.BaseBlockRender;
import appeng.client.render.BlockRenderInfo;
import appeng.client.render.blocks.RenderIOPort;
import appeng.tile.storage.TileIOPort;

public class RenderBlockExIOPort extends RenderIOPort {

    // Keep the extended port's own textures instead of AE2's colored port overlays.
    private final BaseBlockRender<BlockIOPort, TileIOPort> baseRenderer = new BaseBlockRender<>(false, 20);

    @Override
    public void renderInventory(final BlockIOPort block, final ItemStack item, final RenderBlocks renderer,
        final ItemRenderType type, final Object[] data) {
        baseRenderer.renderInventory(block, item, renderer, type, data);
    }

    @Override
    public boolean renderInWorld(final BlockIOPort block, final IBlockAccess world, final int x, final int y,
        final int z, final RenderBlocks renderer) {
        final TileIOPort ti = block.getTileEntity(world, x, y, z);
        final BlockRenderInfo info = block.getRendererInstance();
        if (ti instanceof TileExIOPort port) {
            final IIcon bottom = BlockTexture.ExIOPort_Bottom.getIcon();
            final IIcon side;
            final IIcon top;
            if (port.isActive()) {
                side = BlockTexture.ExIOPort_Side.getIcon();
                top = BlockTexture.ExIOPort_Top.getIcon();
            } else {
                side = BlockTexture.ExIOPort_Side_Off.getIcon();
                top = BlockTexture.ExIOPort_Top_Off.getIcon();
            }
            info.setTemporaryRenderIcons(top, bottom, side, side, side, side);
        }

        final boolean fz = baseRenderer.renderInWorld(block, world, x, y, z, renderer);

        info.setTemporaryRenderIcon(null);

        return fz;
    }
}

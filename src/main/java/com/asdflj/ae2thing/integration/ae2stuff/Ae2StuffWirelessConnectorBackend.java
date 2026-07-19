package com.asdflj.ae2thing.integration.ae2stuff;

import net.bdew.ae2stuff.grid.Security;
import net.bdew.ae2stuff.machines.wireless.BlockWireless;
import net.bdew.ae2stuff.machines.wireless.TileWireless;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.util.Util;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.hooks.TickHandler;
import appeng.me.Grid;
import scala.Option;

public class Ae2StuffWirelessConnectorBackend implements WirelessConnectorBackend {

    private static final String LEFT_COORDINATE_TAG = "#0";
    private static final String RIGHT_COORDINATE_TAG = "#1";
    private static final int LEFT_TILE_INDEX = 0;
    private static final int RIGHT_TILE_INDEX = 1;
    private static final int SINGLE_TILE_RESULT_SIZE = 1;
    private static final int TILE_PAIR_SIZE = 2;
    private static final int NO_PLAYER_ID = -1;

    @Override
    public void writeTiles(EntityPlayer player, IGrid grid, NBTTagList output) {
        visitAvailableTiles(player, grid, tile -> {
            output.appendTag(writeTile(tile));
            return false;
        });
    }

    @Override
    public void setName(EntityPlayer player, IGrid grid, String name, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        TileWireless tile = findTile(player, grid, DimensionalCoord.readFromNBT(tag), null);
        if (tile != null) {
            tile.setCustomName(name);
        }
    }

    @Override
    public void bind(EntityPlayer player, IGrid grid, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        DimensionalCoord leftCoord = DimensionalCoord.readFromNBT((NBTTagCompound) tag.getTag(LEFT_COORDINATE_TAG));
        DimensionalCoord rightCoord = DimensionalCoord.readFromNBT((NBTTagCompound) tag.getTag(RIGHT_COORDINATE_TAG));
        TileWireless[] tiles = findTiles(player, grid, leftCoord, rightCoord);
        if ((tiles[LEFT_TILE_INDEX] != null) && (tiles[RIGHT_TILE_INDEX] != null)) {
            link(player, tiles[LEFT_TILE_INDEX], tiles[RIGHT_TILE_INDEX]);
        }
    }

    @Override
    public void unbind(EntityPlayer player, IGrid grid, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        DimensionalCoord coord = DimensionalCoord.readFromNBT((NBTTagCompound) tag.getTag(LEFT_COORDINATE_TAG));
        TileWireless tile = findTile(player, grid, coord, null);
        if ((tile != null) && tile.isLinked()) {
            tile.doUnlink();
        }
    }

    @Override
    public void setColor(EntityPlayer player, IGrid grid, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        NBTTagCompound data = (NBTTagCompound) tag.getTag(LEFT_COORDINATE_TAG);
        DimensionalCoord coord = DimensionalCoord.readFromNBT(data);
        int colorIndex = data.getShort(Constants.COLOR);
        if ((colorIndex < 0) || (colorIndex >= AEColor.values().length)) {
            return;
        }
        TileWireless tile = findTile(player, grid, coord, null);
        if (tile != null) {
            tile.color_$eq(AEColor.values()[colorIndex]);
            tile.getWorldObject()
                .markBlockForUpdate(coord.x, coord.y, coord.z);
        }
    }

    @Override
    public boolean isWirelessTile(TileEntity tile) {
        return tile instanceof TileWireless;
    }

    private void visitAvailableTiles(EntityPlayer player, IGrid currentGrid, TileVisitor visitor) {
        if ((player == null) || (currentGrid == null)) {
            return;
        }
        int playerId = Security.getPlayerId(player.getGameProfile());
        for (Grid grid : TickHandler.INSTANCE.getGridList()) {
            IMachineSet machines = grid.getMachines(TileWireless.class);
            if (machines.isEmpty()) {
                continue;
            }
            boolean sameGrid = currentGrid.equals(grid);
            for (IGridNode node : machines) {
                TileWireless tile = (TileWireless) node.getGridBlock();
                if (sameGrid) {
                    if (visitor.visit(tile)) {
                        return;
                    }
                    continue;
                }
                int nodePlayerId = node.getPlayerID();
                if ((nodePlayerId == NO_PLAYER_ID) || (nodePlayerId != playerId)) {
                    continue;
                }
                if (visitor.visit(tile)) {
                    return;
                }
            }
        }
    }

    private TileWireless findTile(EntityPlayer player, IGrid grid, DimensionalCoord coord, TileWireless excluded) {
        TileWireless[] result = new TileWireless[SINGLE_TILE_RESULT_SIZE];
        visitAvailableTiles(player, grid, tile -> {
            if ((tile != excluded) && Util.isSameDimensionalCoord(tile.getLocation(), coord)) {
                result[LEFT_TILE_INDEX] = tile;
                return true;
            }
            return false;
        });
        return result[LEFT_TILE_INDEX];
    }

    private TileWireless[] findTiles(EntityPlayer player, IGrid grid, DimensionalCoord leftCoord,
        DimensionalCoord rightCoord) {
        TileWireless[] result = new TileWireless[TILE_PAIR_SIZE];
        visitAvailableTiles(player, grid, tile -> {
            if ((result[LEFT_TILE_INDEX] == null) && Util.isSameDimensionalCoord(tile.getLocation(), leftCoord)) {
                result[LEFT_TILE_INDEX] = tile;
            } else if (result[RIGHT_TILE_INDEX] == null) {
                if (Util.isSameDimensionalCoord(tile.getLocation(), rightCoord)) {
                    result[RIGHT_TILE_INDEX] = tile;
                }
            }
            return (result[LEFT_TILE_INDEX] != null) && (result[RIGHT_TILE_INDEX] != null);
        });
        if ((result[LEFT_TILE_INDEX] != null) && (result[LEFT_TILE_INDEX] == result[RIGHT_TILE_INDEX])) {
            result[RIGHT_TILE_INDEX] = findTile(player, grid, rightCoord, result[LEFT_TILE_INDEX]);
        }
        return result;
    }

    private NBTTagCompound writeTile(TileWireless tile) {
        NBTTagCompound data = new NBTTagCompound();
        tile.getLocation()
            .writeToNBT(data);
        data.setString(Constants.NAME, tile.hasCustomName() ? tile.getCustomName() : BlockWireless.getLocalizedName());
        data.setInteger(
            Constants.COLOR,
            tile.getColor()
                .ordinal());
        data.setBoolean(Constants.IS_LINKED, tile.isLinked());
        data.setInteger(
            Constants.USED_CHANNELS,
            (tile.connection() != null) ? tile.connection()
                .getUsedChannels() : 0);
        if (tile.isLinked()) {
            NBTTagCompound linked = new NBTTagCompound();
            Option<TileWireless> other = tile.getLink();
            if (!other.isEmpty()) {
                other.get()
                    .getLocation()
                    .writeToNBT(linked);
                data.setTag(Constants.LINK, linked);
            }
        }
        return data;
    }

    private void link(EntityPlayer player, TileWireless left, TileWireless right) {
        if (left == right) {
            return;
        }
        if (left.isLinked()) {
            left.doUnlink();
        }
        if (right.isLinked()) {
            right.doUnlink();
        }
        try {
            left.doLink(right);
        } catch (Exception e) {
            left.doUnlink();
            right.doUnlink();
            ChatComponentText message = new ChatComponentText(
                StatCollector.translateToLocal("ae2stuff.wireless.tool.failed") + ": " + e.getMessage());
            message.getChatStyle()
                .setColor(EnumChatFormatting.RED);
            player.addChatComponentMessage(message);
        }
    }

    @FunctionalInterface
    private interface TileVisitor {

        boolean visit(TileWireless tile);
    }
}

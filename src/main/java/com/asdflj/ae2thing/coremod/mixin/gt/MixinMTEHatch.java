package com.asdflj.ae2thing.coremod.mixin.gt;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.asdflj.ae2thing.util.InterfaceRecipeMaps;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.common.tileentities.machines.IHatchWatcher;

@Mixin(value = MTEHatch.class, remap = false)
public abstract class MixinMTEHatch implements InterfaceRecipeMaps.HatchControllers {

    @Shadow
    @Final
    protected List<IHatchWatcher> watchers;

    @Shadow
    private ChunkCoordinates ae2CraftingIconOwner;

    @Override
    public List<?> ae2thing$getRecipeMapControllers() {
        List<Object> controllers = new ArrayList<>(this.watchers);
        IGregTechTileEntity base = ((MTEHatch) (Object) this).getBaseMetaTileEntity();
        World world = base == null ? null : base.getWorld();
        ChunkCoordinates owner = this.ae2CraftingIconOwner;
        if (world != null && owner != null && world.blockExists(owner.posX, owner.posY, owner.posZ)) {
            TileEntity tile = world.getTileEntity(owner.posX, owner.posY, owner.posZ);
            if (tile instanceof IGregTechTileEntity gtTile) {
                Object controller = gtTile.getMetaTileEntity();
                if (controller != null && !controllers.contains(controller)) controllers.add(controller);
            }
        }
        return controllers;
    }
}

package com.asdflj.ae2thing.coremod.mixin.ae;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.ae2thing.util.FluidPatternCompatibility;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedUltimatePattern;

@Mixin(value = ItemEncodedUltimatePattern.class, remap = false)
public abstract class MixinItemEncodedUltimatePattern {

    @Inject(method = "getPatternForItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void ae2thing$readFluidMarkers(ItemStack pattern, World world,
        CallbackInfoReturnable<ICraftingPatternDetails> cir) {
        ItemStack normalized = FluidPatternCompatibility.normalizedCopy(pattern);
        if (normalized != null) cir.setReturnValue(FluidPatternCompatibility.parse(pattern, normalized));
    }
}

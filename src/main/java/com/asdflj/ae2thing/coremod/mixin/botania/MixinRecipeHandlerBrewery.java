package com.asdflj.ae2thing.coremod.mixin.botania;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.asdflj.ae2thing.crossmod.nei.BreweryRecipeSearch;

import vazkii.botania.client.integration.nei.recipe.RecipeHandlerBrewery;

@Mixin(RecipeHandlerBrewery.class)
public abstract class MixinRecipeHandlerBrewery {

    @Redirect(
        method = "loadCraftingRecipes(Lnet/minecraft/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isItemEqual(Lnet/minecraft/item/ItemStack;)Z",
            remap = true),
        remap = false,
        require = 0)
    private boolean ae2thing$matchesBrewOutput(ItemStack result, ItemStack filledContainer) {
        return BreweryRecipeSearch.matchesItem(result, filledContainer);
    }
}

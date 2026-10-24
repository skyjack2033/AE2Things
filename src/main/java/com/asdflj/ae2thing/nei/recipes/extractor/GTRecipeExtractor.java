package com.asdflj.ae2thing.nei.recipes.extractor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.nei.object.IRecipeExtractor;
import com.asdflj.ae2thing.nei.object.OrderStack;
import com.asdflj.ae2thing.nei.recipes.FluidRecipe;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.StackInfo;
import gregtech.api.recipe.RecipeCategory;
import gregtech.nei.GTNEIDefaultHandler.FixedPositionedStack;

public class GTRecipeExtractor implements IRecipeExtractor {

    public static void registerRecipeMaps() {
        GTRecipeExtractor extractor = new GTRecipeExtractor();
        for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
            if (category.recipeMap.getFrontend()
                .getNEIProperties().registerNEI) {
                FluidRecipe.addRecipeMapIfAbsent(category.unlocalizedName, extractor);
            }
        }
    }

    @Override
    public List<OrderStack<?>> getInputIngredients(List<PositionedStack> rawInputs) {
        return extract(rawInputs);
    }

    @Override
    public List<OrderStack<?>> getOutputIngredients(List<PositionedStack> rawOutputs) {
        return extract(rawOutputs);
    }

    private static List<OrderStack<?>> extract(List<PositionedStack> stacks) {
        List<OrderStack<?>> result = new ArrayList<>();
        for (PositionedStack positioned : stacks) {
            if (positioned == null) continue;
            FluidStack fluid = getFluid(positioned);
            if (fluid != null) {
                result.add(new OrderStack<>(fluid.copy(), result.size()));
            } else if (positioned.item != null) {
                ItemStack item = positioned.item.copy();
                // GT can render every stack as one, including zero-sized non-consumable inputs.
                if (positioned instanceof FixedPositionedStack fixed && !fixed.renderRealStackSize) {
                    item.stackSize = fixed.realStackSize;
                }
                result.add(new OrderStack<>(item, result.size()));
            }
        }
        return result;
    }

    private static FluidStack getFluid(PositionedStack positioned) {
        // NEI owns both its current fluid displays and the legacy GT display adapter.
        // Filled buckets and cells are still item ingredients, not raw recipe fluids.
        return StackInfo.isFluidDisplayItem(positioned.item) ? StackInfo.getFluid(positioned.item) : null;
    }
}

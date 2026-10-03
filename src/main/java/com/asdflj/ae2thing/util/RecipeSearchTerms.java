package com.asdflj.ae2thing.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.asdflj.ae2thing.nei.object.OrderStack;

/** Builds search terms shared by GT's circuit/item suffixes and PH's older display names. */
final class RecipeSearchTerms {

    private RecipeSearchTerms() {}

    static String appendNonConsumedItems(String recipeName, List<OrderStack<?>> inputs,
        Predicate<ItemStack> isIntegratedCircuit) {
        Set<String> terms = new LinkedHashSet<>();
        for (OrderStack<?> input : inputs) {
            if (input == null || !(input.getStack() instanceof ItemStack stack) || stack.stackSize != 0) {
                continue;
            }
            if (isIntegratedCircuit.test(stack)) {
                terms.add(Integer.toString(stack.getItemDamage()));
            } else {
                String name = shortItemName(stack.getDisplayName());
                if (!name.isEmpty()) terms.add(name);
            }
        }
        return terms.isEmpty() ? recipeName : recipeName + " " + String.join(" ", terms);
    }

    private static String shortItemName(String displayName) {
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(displayName)
            .trim();
        // GT abbreviates "Mold (Ingot)" to "Ingot". PH's full item name also contains that term.
        if (!name.endsWith(")")) return name;
        int open = name.lastIndexOf('(');
        if (open < 0) return name;
        String inner = name.substring(open + 1, name.length() - 1)
            .trim();
        return inner.isEmpty() ? name : inner;
    }
}

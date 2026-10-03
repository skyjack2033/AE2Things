package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.asdflj.ae2thing.nei.object.OrderStack;

public class RecipeSearchTermsTest {

    private final Item circuit = new NamedItem("Programmed Circuit");

    @Test
    public void findsTheCircuitAfterAMoldWithoutUsingTheMoldDamage() {
        ItemStack mold = new ItemStack(new NamedItem("Mold (Ingot)"), 0, 32300);
        ItemStack programmed = new ItemStack(circuit, 0, 5);

        assertEquals("Extruder Ingot 5", suggest("Extruder", item(mold, 0), item(programmed, 1)));
        assertEquals(0, mold.stackSize);
        assertEquals(32300, mold.getItemDamage());
        assertEquals(0, programmed.stackSize);
        assertEquals(5, programmed.getItemDamage());
    }

    @Test
    public void includesAllDistinctCircuitAndNonConsumedItemTerms() {
        assertEquals(
            "Assembler 1 11 Lens",
            suggest(
                "Assembler",
                item(new ItemStack(circuit, 0, 1), 0),
                item(new ItemStack(circuit, 0, 11), 1),
                item(new ItemStack(circuit, 0, 1), 2),
                item(new ItemStack(new NamedItem("Lens"), 0, 50), 3),
                item(new ItemStack(new NamedItem("Lens"), 0, 50), 4)));
    }

    @Test
    public void excludesConsumedCircuitsOtherIngredientsAndMissingInputs() {
        assertEquals(
            "Assembly Line",
            suggest(
                "Assembly Line",
                null,
                item(new ItemStack(circuit, 1, 5), 0),
                item(new ItemStack(new NamedItem("Steel Plate"), 4), 1),
                new OrderStack<>(new Object(), 2)));
    }

    @Test
    public void usesShortNamesThatMatchBothGtAndPhSuffixes() {
        assertEquals(
            "Extruder Gear Copper Mold ()",
            suggest(
                "Extruder",
                item(new ItemStack(new NamedItem("\u00a7aExtruder Shape (Gear)"), 0, 32303), 0),
                item(new ItemStack(new NamedItem("Copper"), 0, 0), 1),
                item(new ItemStack(new NamedItem("Mold ()"), 0, 100), 2)));
    }

    @Test
    public void keepsTheFullRecipeNameWhenThereAreNoExtraTerms() {
        assertEquals("Large Chemical Reactor", suggest("Large Chemical Reactor"));
        assertEquals(
            "Large Chemical Reactor",
            suggest("Large Chemical Reactor", item(new ItemStack(new NamedItem("  "), 0), 0)));
    }

    private String suggest(String name, OrderStack<?>... inputs) {
        List<OrderStack<?>> list = Arrays.asList(inputs);
        return RecipeSearchTerms.appendNonConsumedItems(name, list, stack -> stack.getItem() == circuit);
    }

    private static OrderStack<?> item(ItemStack stack, int index) {
        return new OrderStack<>(stack, index);
    }

    private static class NamedItem extends Item {

        private final String name;

        private NamedItem(String name) {
            this.name = name;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return name;
        }
    }
}

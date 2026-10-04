package com.asdflj.ae2thing.nei.recipes.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.junit.Test;

import com.asdflj.ae2thing.nei.object.OrderStack;

import cpw.mods.fml.common.registry.RegistryDelegate;
import gregtech.nei.GTNEIDefaultHandler.FixedPositionedStack;

public class GTRecipeExtractorTest {

    private final GTRecipeExtractor extractor = new GTRecipeExtractor();

    @Test
    public void restoresHiddenItemCountsAndRetainsZeroQuantityCircuitNbt() throws Exception {
        ItemStack displayedCircuit = new ItemStack(new Item().setHasSubtypes(true), 1, 24);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("configuration", "keep circuit selection");
        displayedCircuit.setTagCompound(tag);
        ItemStack displayedIngredient = new ItemStack(new Item(), 1);

        List<OrderStack<?>> result = extractor.getInputIngredients(
            Arrays.asList(fixed(displayedCircuit, 0, false), fixed(displayedIngredient, 128, false)));

        ItemStack circuit = (ItemStack) result.get(0)
            .getStack();
        ItemStack ingredient = (ItemStack) result.get(1)
            .getStack();
        assertEquals(0, circuit.stackSize);
        assertEquals(24, circuit.getItemDamage());
        assertEquals(tag, circuit.getTagCompound());
        assertNotSame(tag, circuit.getTagCompound());
        assertEquals(128, ingredient.stackSize);
        assertEquals(1, displayedCircuit.stackSize);
        assertEquals(1, displayedIngredient.stackSize);
        assertNotSame(displayedCircuit, circuit);
        assertNotSame(displayedIngredient, ingredient);
    }

    @Test
    public void keepsTheSelectedAlternativeCountWhenRealCountsAreVisible() throws Exception {
        ItemStack selected = new ItemStack(new Item(), 16);
        FixedPositionedStack positioned = fixed(selected, 4, true);

        ItemStack result = (ItemStack) extractor.getInputIngredients(Collections.singletonList(positioned))
            .get(0)
            .getStack();

        assertEquals(16, result.stackSize);
        assertEquals(16, selected.stackSize);
        assertNotSame(selected, result);
    }

    @Test
    public void extractsTheSelectedFluidAmountAndIndependentNbt() throws Exception {
        FluidStack primary = fluid("primary", 1000);
        FluidStack selected = fluid("selected", 144);
        selected.tag = new NBTTagCompound();
        selected.tag.setString("purity", "high");
        FixedPositionedStack positioned = fluids(Arrays.asList(primary, selected), 1);

        FluidStack result = (FluidStack) extractor.getInputIngredients(Collections.singletonList(positioned))
            .get(0)
            .getStack();

        assertSame(selected.getFluid(), result.getFluid());
        assertEquals(144, result.amount);
        assertEquals(selected.tag, result.tag);
        assertNotSame(selected, result);
        assertNotSame(selected.tag, result.tag);
        result.amount = 1;
        result.tag.setString("purity", "changed");
        assertEquals(144, selected.amount);
        assertEquals("high", selected.tag.getString("purity"));
    }

    @Test
    public void usesTheDefaultFluidAndKeepsAZeroAmount() throws Exception {
        FluidStack primary = fluid("non_consumed_fluid", 0);
        FixedPositionedStack positioned = fluids(Arrays.asList(primary, fluid("alternative", 2000)), -1);

        FluidStack result = (FluidStack) extractor.getInputIngredients(Collections.singletonList(positioned))
            .get(0)
            .getStack();

        assertSame(primary.getFluid(), result.getFluid());
        assertEquals(0, result.amount);
        assertNotSame(primary, result);
    }

    @Test
    public void preservesMachineOrderAndRepeatedInputsWithoutUsingCoordinates() throws Exception {
        ItemStack ingredient = new ItemStack(new Item(), 3);
        FixedPositionedStack first = fixed(ingredient, 3, true);
        first.relx = 200;
        first.rely = 100;
        FixedPositionedStack second = fluids(Collections.singletonList(fluid("middle", 500)), -1);
        FixedPositionedStack third = fixed(ingredient.copy(), 3, true);
        third.relx = 1;
        third.rely = 1;

        List<OrderStack<?>> result = extractor.getInputIngredients(Arrays.asList(first, null, second, third));

        assertEquals(3, result.size());
        assertSame(
            ingredient.getItem(),
            ((ItemStack) result.get(0)
                .getStack()).getItem());
        assertTrue(
            result.get(1)
                .getStack() instanceof FluidStack);
        assertSame(
            ingredient.getItem(),
            ((ItemStack) result.get(2)
                .getStack()).getItem());
        assertNotSame(
            result.get(0)
                .getStack(),
            result.get(2)
                .getStack());
        for (int i = 0; i < result.size(); i++) {
            assertEquals(
                i,
                result.get(i)
                    .getIndex());
        }
    }

    @Test
    public void extractsMultipleOtherOutputsAfterANullMainResult() throws Exception {
        ItemStack item = new ItemStack(new Item(), 2);
        FluidStack fluid = fluid("output", 2000);
        // FluidRecipe combines the nullable primary result with GT's otherStacks output list.
        List<OrderStack<?>> result = extractor.getOutputIngredients(
            Arrays.asList(null, fixed(item, 2, true), fluids(Collections.singletonList(fluid), -1)));

        assertEquals(2, result.size());
        assertEquals(
            0,
            result.get(0)
                .getIndex());
        assertEquals(
            1,
            result.get(1)
                .getIndex());
        assertSame(
            item.getItem(),
            ((ItemStack) result.get(0)
                .getStack()).getItem());
        assertEquals(
            2,
            ((ItemStack) result.get(0)
                .getStack()).stackSize);
        assertEquals(
            2000,
            ((FluidStack) result.get(1)
                .getStack()).amount);
        assertNotSame(
            item,
            result.get(0)
                .getStack());
        assertNotSame(
            fluid,
            result.get(1)
                .getStack());
    }

    private static FixedPositionedStack fixed(ItemStack item, int realCount, boolean renderReal) throws Exception {
        // GT's constructor initializes ore unification and game registries. Populate the actual pinned class fields
        // directly so these tests exercise extraction without starting Minecraft or changing global GT state.
        FixedPositionedStack stack = allocate(FixedPositionedStack.class);
        stack.item = item;
        stack.items = new ItemStack[] { item };
        setField(FixedPositionedStack.class, stack, "realStackSize", realCount);
        setField(FixedPositionedStack.class, stack, "renderRealStackSize", renderReal);
        setField(FixedPositionedStack.class, stack, "fluidAlternatives", Collections.emptyList());
        setField(FixedPositionedStack.class, stack, "selectedFluidIndex", -1);
        return stack;
    }

    private static FixedPositionedStack fluids(List<FluidStack> alternatives, int selected) throws Exception {
        FixedPositionedStack stack = fixed(new ItemStack(new Item(), 1), 1, false);
        setField(FixedPositionedStack.class, stack, "fluidAlternatives", Collections.unmodifiableList(alternatives));
        setField(FixedPositionedStack.class, stack, "selectedFluidIndex", selected);
        return stack;
    }

    private static FluidStack fluid(String name, int amount) throws Exception {
        return FluidFixture.create(new Fluid(name), amount, null);
    }

    private static void setField(Class<?> owner, Object instance, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        return type.cast(
            unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(unsafe, type));
    }

    private static class FluidFixture extends FluidStack {

        private FluidFixture() {
            super((Fluid) null, 0);
        }

        private static FluidFixture create(Fluid fluid, int amount, NBTTagCompound tag) throws Exception {
            // Forge's regular FluidStack constructor requires FML's global registry. Keep only the fluid delegate
            // and copy contract needed by the extractor, without registering fake fluids in shared test state.
            FluidFixture stack = allocate(FluidFixture.class);
            stack.amount = amount;
            stack.tag = tag == null ? null : (NBTTagCompound) tag.copy();
            setField(FluidStack.class, stack, "fluid", fluid);
            setField(FluidStack.class, stack, "fluidDelegate", new RegistryDelegate.Delegate<>(fluid, Fluid.class));
            return stack;
        }

        @Override
        public FluidStack copy() {
            try {
                return create(getFluid(), amount, tag);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

}

package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import appeng.api.storage.data.IAEStack;

public class PatternStackCodecTest {

    @Test
    public void processingSchemaUsesNativeStackTypesAndKeepsInputSlots() {
        IAEStack<?> fluid = stack("fluid", 3_000_000_000L);
        NBTTagCompound data = PatternStackCodec.processingData(
            new IAEStack<?>[] { fluid, null, stack("item", 2) },
            new IAEStack<?>[] { null, fluid },
            false);

        assertNotNull(data);
        assertFalse(data.getBoolean("crafting"));
        assertFalse(data.getBoolean("substitute"));
        assertFalse(data.hasKey("prioritize"));
        NBTTagList inputs = data.getTagList("in", 10);
        NBTTagList outputs = data.getTagList("out", 10);
        assertEquals(3, inputs.tagCount());
        assertEquals(1, outputs.tagCount());
        assertEquals(
            "fluid",
            inputs.getCompoundTagAt(0)
                .getString("StackType"));
        assertEquals(
            "ae2thing_schema_fixture",
            inputs.getCompoundTagAt(0)
                .getString("FluidName"));
        assertEquals(
            3_000_000_000L,
            inputs.getCompoundTagAt(0)
                .getLong("Cnt"));
        assertTrue(
            inputs.getCompoundTagAt(1)
                .hasNoTags());
        assertEquals(
            "item",
            inputs.getCompoundTagAt(2)
                .getString("StackType"));
        assertEquals(
            "fluid",
            outputs.getCompoundTagAt(0)
                .getString("StackType"));
    }

    @Test
    public void fluidInputsAreExactWithoutChangingPatternSubstitution() {
        NBTTagCompound data = PatternStackCodec.processingData(
            new IAEStack<?>[] { stack("item", 1), stack("fluid", 144) },
            new IAEStack<?>[] { stack("item", 1) },
            true);

        assertNotNull(data);
        assertFalse(data.getBoolean("substitute"));
        assertTrue(data.getBoolean("beSubstitute"));
    }

    @Test
    public void fluidOutputsAlsoRequireExactProcessingInputs() {
        NBTTagCompound data = PatternStackCodec.processingData(
            new IAEStack<?>[] { stack("item", 1) },
            new IAEStack<?>[] { stack("fluid", 144) },
            true);

        assertNotNull(data);
        assertFalse(data.getBoolean("substitute"));
        assertTrue(data.getBoolean("beSubstitute"));
    }

    @Test
    public void processingAlwaysUsesExactInputsButPreservesPatternSubstitution() {
        for (boolean beSubstitute : new boolean[] { false, true }) {
            NBTTagCompound data = PatternStackCodec.processingData(
                new IAEStack<?>[] { stack("item", 1) },
                new IAEStack<?>[] { stack("item", 1) },
                beSubstitute);

            assertNotNull(data);
            assertFalse(data.getBoolean("substitute"));
            assertEquals(beSubstitute, data.getBoolean("beSubstitute"));
        }
    }

    @Test
    public void itemMetadataRemainsExactWithPatternSubstitutionEnabled() {
        NBTTagCompound ingredient = new NBTTagCompound();
        ingredient.setString("StackType", "item");
        ingredient.setString("id", "ae2thing_metadata_fixture");
        ingredient.setShort("Damage", (short) 32036);
        ingredient.setLong("Cnt", 64);
        NBTTagCompound itemData = new NBTTagCompound();
        itemData.setString("variant", "waferILC");
        ingredient.setTag("tag", itemData);

        NBTTagCompound data = PatternStackCodec.processingData(
            new IAEStack<?>[] { null, stack(ingredient) },
            new IAEStack<?>[] { stack("item", 1) },
            true);

        assertNotNull(data);
        assertFalse(data.getBoolean("substitute"));
        assertTrue(data.getBoolean("beSubstitute"));
        NBTTagList inputs = data.getTagList("in", 10);
        assertEquals(2, inputs.tagCount());
        assertTrue(
            inputs.getCompoundTagAt(0)
                .hasNoTags());
        assertEquals(ingredient, inputs.getCompoundTagAt(1));
    }

    @Test
    public void processingKeepsLastInputAndOutputSlotsWithoutEnablingInputSubstitution() {
        IAEStack<?>[] inputs = new IAEStack<?>[32];
        IAEStack<?>[] outputs = new IAEStack<?>[32];
        inputs[31] = stack("item", 64);
        outputs[31] = stack("item", 1);

        NBTTagCompound data = PatternStackCodec.processingData(inputs, outputs, true);
        assertNotNull(data);
        assertFalse(data.getBoolean("substitute"));
        assertTrue(data.getBoolean("beSubstitute"));
        NBTTagList in = data.getTagList("in", 10);
        NBTTagList out = data.getTagList("out", 10);
        assertEquals(32, in.tagCount());
        assertEquals(1, out.tagCount());
        assertEquals(
            64,
            in.getCompoundTagAt(31)
                .getLong("Cnt"));
        assertEquals(
            1,
            out.getCompoundTagAt(0)
                .getLong("Cnt"));
    }

    @Test
    public void rejectsMissingOrEmptyProcessingSides() {
        assertNull(PatternStackCodec.processingData(null, new IAEStack<?>[] { stack("item", 1) }, false));
        assertNull(
            PatternStackCodec.processingData(new IAEStack<?>[] { stack("item", 1) }, new IAEStack<?>[0], false));
        assertNull(
            PatternStackCodec.processingData(
                new IAEStack<?>[] { stack("item", 0) },
                new IAEStack<?>[] { stack("item", 1) },
                false));
    }

    @Test
    public void ignoresNullOutputsInsteadOfWritingInvalidEmptyEntries() {
        NBTTagCompound data = PatternStackCodec.processingData(
            new IAEStack<?>[] { stack("item", 1) },
            new IAEStack<?>[] { null, stack("item", 1), null },
            false);

        assertNotNull(data);
        assertEquals(
            1,
            data.getTagList("out", 10)
                .tagCount());
    }

    private static IAEStack<?> stack(String type, long amount) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("StackType", type);
        if ("fluid".equals(type)) tag.setString("FluidName", "ae2thing_schema_fixture");
        tag.setLong("Cnt", amount);
        return stack(tag);
    }

    private static IAEStack<?> stack(NBTTagCompound tag) {
        Map<String, Object> values = new HashMap<>();
        values.put("getStackSize", tag.getLong("Cnt"));
        values.put("toNBTGeneric", (Supplier<NBTTagCompound>) () -> (NBTTagCompound) tag.copy());
        values.put("isItem", "item".equals(tag.getString("StackType")));
        values.put("isFluid", "fluid".equals(tag.getString("StackType")));
        return ProxyFactory.create(IAEStack.class, values);
    }

    private static final class ProxyFactory {

        private static <T> T create(Class<T> type, Map<String, Object> values) {
            return type.cast(
                java.lang.reflect.Proxy
                    .newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
                        Object value = values.get(method.getName());
                        if (value instanceof Supplier<?>supplier) return supplier.get();
                        if (value != null) return value;
                        if (method.getName()
                            .equals("toString")) return "test-stack";
                        if (method.getName()
                            .equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName()
                            .equals("equals")) return proxy == args[0];
                        throw new AssertionError("Unexpected stack method: " + method.getName());
                    }));
        }
    }
}

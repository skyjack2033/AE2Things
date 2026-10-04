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
            true,
            false,
            true);

        assertNotNull(data);
        assertFalse(data.getBoolean("crafting"));
        assertTrue(data.getBoolean("substitute"));
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
    public void rejectsMissingOrEmptyProcessingSides() {
        assertNull(PatternStackCodec.processingData(null, new IAEStack<?>[] { stack("item", 1) }, false, false, false));
        assertNull(
            PatternStackCodec
                .processingData(new IAEStack<?>[] { stack("item", 1) }, new IAEStack<?>[0], false, false, false));
        assertNull(
            PatternStackCodec.processingData(
                new IAEStack<?>[] { stack("item", 0) },
                new IAEStack<?>[] { stack("item", 1) },
                false,
                false,
                false));
    }

    @Test
    public void ignoresNullOutputsInsteadOfWritingInvalidEmptyEntries() {
        NBTTagCompound data = PatternStackCodec.processingData(
            new IAEStack<?>[] { stack("item", 1) },
            new IAEStack<?>[] { null, stack("item", 1), null },
            false,
            false,
            false);

        assertNotNull(data);
        assertEquals(
            1,
            data.getTagList("out", 10)
                .tagCount());
    }

    private static IAEStack<?> stack(String type, long amount) {
        Map<String, Object> values = new HashMap<>();
        values.put("getStackSize", amount);
        values.put("toNBTGeneric", (Supplier<NBTTagCompound>) () -> {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("StackType", type);
            if ("fluid".equals(type)) tag.setString("FluidName", "ae2thing_schema_fixture");
            tag.setLong("Cnt", amount);
            return tag;
        });
        values.put("isItem", "item".equals(type));
        values.put("isFluid", "fluid".equals(type));
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

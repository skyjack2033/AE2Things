package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

public class PatternModifierStacksTest {

    @Test
    public void nativeFluidReplacementRetainsLongAmountAndInputSlot() {
        IAEStack<?> original = stack("fluid", "water", 3_000_000_000L);
        IAEStack<?> target = stack("fluid", "steam", 1000);
        IAEStack<?>[] result = PatternModifierStacks
            .replace(new IAEStack<?>[] { null, original }, stack("fluid", "water", 1), target, slot -> true);

        assertNotNull(result);
        assertNull(result[0]);
        assertTrue(result[1] instanceof IAEFluidStack);
        assertEquals("steam", result[1].getUnlocalizedName());
        assertEquals(3_000_000_000L, result[1].getStackSize());
        assertEquals(1000, target.getStackSize());
        assertEquals(3_000_000_000L, original.getStackSize());
        assertNotSame(target, result[1]);
    }

    @Test
    public void replacingAnItemKeepsFluidInputsAndOutputsInNativeEncoding() {
        IAEStack<?> source = stack("item", "copper", 1);
        IAEStack<?> target = stack("item", "tin", 1);
        IAEStack<?>[] inputs = PatternModifierStacks.replace(
            new IAEStack<?>[] { stack("fluid", "water", 144), null, stack("item", "copper", 7) },
            source,
            target,
            slot -> true);
        IAEStack<?>[] outputs = PatternModifierStacks.replace(
            new IAEStack<?>[] { stack("fluid", "steam", 3_000_000_000L), stack("item", "copper", 2) },
            source,
            target,
            slot -> true);

        NBTTagCompound data = PatternStackCodec.processingData(inputs, outputs, true, false);
        assertNotNull(data);
        assertFalse(data.getBoolean("substitute"));
        NBTTagList in = data.getTagList("in", 10);
        NBTTagList out = data.getTagList("out", 10);
        assertEquals(3, in.tagCount());
        assertEquals(2, out.tagCount());
        assertEquals(
            "fluid",
            in.getCompoundTagAt(0)
                .getString("StackType"));
        assertEquals(
            "water",
            in.getCompoundTagAt(0)
                .getString("FluidName"));
        assertEquals(
            144,
            in.getCompoundTagAt(0)
                .getLong("Cnt"));
        assertTrue(
            in.getCompoundTagAt(1)
                .hasNoTags());
        assertEquals(
            "item",
            in.getCompoundTagAt(2)
                .getString("StackType"));
        assertEquals("tin", inputs[2].getUnlocalizedName());
        assertEquals(7, inputs[2].getStackSize());
        assertEquals(
            "fluid",
            out.getCompoundTagAt(0)
                .getString("StackType"));
        assertEquals(
            3_000_000_000L,
            out.getCompoundTagAt(0)
                .getLong("Cnt"));
        assertEquals("tin", outputs[1].getUnlocalizedName());
        assertEquals(2, outputs[1].getStackSize());
    }

    @Test
    public void matchingIgnoresAmountButKeepsItemsAndFluidsDistinct() {
        assertTrue(PatternModifierStacks.matches(stack("fluid", "water", 1), stack("fluid", "water", 1000)));
        assertFalse(PatternModifierStacks.matches(stack("fluid", "water", 1), stack("fluid", "steam", 1)));
        assertFalse(PatternModifierStacks.matches(stack("fluid", "water", 1), stack("item", "water", 1)));
    }

    @Test
    public void removalKeepsUnmatchedFluidAndExistingEmptySlots() {
        IAEStack<?>[] result = PatternModifierStacks.replace(
            new IAEStack<?>[] { stack("fluid", "water", 144), null, stack("fluid", "steam", 1000) },
            stack("fluid", "water", 1),
            null,
            slot -> true);

        assertNotNull(result);
        assertEquals(3, result.length);
        assertNull(result[0]);
        assertNull(result[1]);
        assertEquals("steam", result[2].getUnlocalizedName());
        assertEquals(1000, result[2].getStackSize());
    }

    @Test
    public void rejectedCraftingReplacementLeavesIngredientIntact() {
        IAEStack<?> ingredient = stack("item", "copper", 4);
        IAEStack<?>[] result = PatternModifierStacks.replace(
            new IAEStack<?>[] { ingredient },
            stack("item", "copper", 1),
            stack("fluid", "water", 1),
            slot -> false);

        assertNotNull(result);
        assertTrue(result[0] instanceof IAEItemStack);
        assertEquals("copper", result[0].getUnlocalizedName());
        assertEquals(4, result[0].getStackSize());
        assertNotSame(ingredient, result[0]);
    }

    private static IAEStack<?> stack(String kind, String name, long amount) {
        Class<?> type = "fluid".equals(kind) ? IAEFluidStack.class : IAEItemStack.class;
        return (IAEStack<?>) Proxy
            .newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new TestStack(kind, name, amount));
    }

    private static final class TestStack implements InvocationHandler {

        private final String kind;
        private final String name;
        private long amount;

        private TestStack(String kind, String name, long amount) {
            this.kind = kind;
            this.name = name;
            this.amount = amount;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getItem" -> null;
                case "getStackSize" -> amount;
                case "getUnlocalizedName" -> name;
                case "copy" -> stack(kind, name, amount);
                case "isItem" -> "item".equals(kind);
                case "isFluid" -> "fluid".equals(kind);
                case "setStackSize" -> {
                    amount = (long) args[0];
                    yield proxy;
                }
                case "isSameType" -> {
                    if (args[0] == null || !Proxy.isProxyClass(args[0].getClass())) yield false;
                    TestStack other = (TestStack) Proxy.getInvocationHandler(args[0]);
                    yield kind.equals(other.kind) && name.equals(other.name);
                }
                case "toNBTGeneric" -> {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setString("StackType", kind);
                    tag.setString("fluid".equals(kind) ? "FluidName" : "id", name);
                    tag.setLong("Cnt", amount);
                    yield tag;
                }
                case "toString" -> kind + ":" + name;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> throw new AssertionError("Unexpected stack method: " + method.getName());
            };
        }
    }
}

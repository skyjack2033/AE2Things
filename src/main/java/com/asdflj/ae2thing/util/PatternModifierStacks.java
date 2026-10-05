package com.asdflj.ae2thing.util;

import java.util.function.IntPredicate;

import appeng.api.storage.data.IAEStack;

/** Shared matching and replacement for the modifier's item and native fluid ingredients. */
public final class PatternModifierStacks {

    private PatternModifierStacks() {}

    public static boolean matches(IAEStack<?> stack, IAEStack<?> source) {
        IAEStack<?> left = PatternStackCodec.normalize(stack);
        IAEStack<?> right = PatternStackCodec.normalize(source);
        return left != null && right != null && left.isSameType(right);
    }

    /** Keeps each ingredient's amount and position; a null target removes allowed processing ingredients. */
    public static IAEStack<?>[] replace(IAEStack<?>[] stacks, IAEStack<?> source, IAEStack<?> target,
        IntPredicate canReplace) {
        if (stacks == null) return null;
        IAEStack<?> nativeSource = PatternStackCodec.normalize(source);
        IAEStack<?> nativeTarget = PatternStackCodec.normalize(target);
        if (nativeSource == null || target != null && nativeTarget == null) return null;

        IAEStack<?>[] result = new IAEStack<?>[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            IAEStack<?> original = PatternStackCodec.normalize(stacks[i]);
            // Do not silently drop an invalid legacy fluid wrapper from the recipe.
            if (stacks[i] != null && original == null) return null;
            if (original == null) continue;

            if (matches(original, nativeSource) && canReplace.test(i)) {
                if (nativeTarget != null) {
                    result[i] = nativeTarget.copy();
                    result[i].setStackSize(original.getStackSize());
                }
            } else {
                result[i] = original.copy();
            }
        }
        return result;
    }
}

package com.asdflj.ae2thing.inventory.gui;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Self-contained encoder/decoder for the inventory source carried in a GUI-open x coordinate.
 * AE2FluidCraft-Rework's {@code Util.GuiHelper} dropped the player/baubles distinction (its enum is
 * now {@code TILE}/{@code ITEM}), so AE2Things owns this small bit-packing to keep routing terminal
 * items between the main inventory and Baubles slots.
 */
public enum GuiBridgeInvType {

    PLAYER_INV,
    PLAYER_BAUBLES;

    private static final int TYPE_SHIFT = 29;
    private static final int TYPE_MASK = 1;
    private static final int ENCODED_FLAG = 1 << 30;
    private static final int ENCODED_HEADER_MASK = ENCODED_FLAG | (TYPE_MASK << TYPE_SHIFT);
    private static final int SLOT_LIMIT = 1 << 28;

    public static int encode(int slot, GuiBridgeInvType type) {
        if (Math.abs(slot) > SLOT_LIMIT) {
            throw new IllegalArgumentException("slot out of range");
        }
        return ENCODED_FLAG | (type.ordinal() << TYPE_SHIFT) | slot;
    }

    public static ImmutablePair<GuiBridgeInvType, Integer> decode(int value) {
        if (Math.abs(value) > SLOT_LIMIT) {
            return new ImmutablePair<>(
                values()[(value >> TYPE_SHIFT) & TYPE_MASK],
                value - (ENCODED_HEADER_MASK & value));
        }
        return new ImmutablePair<>(PLAYER_INV, value);
    }
}

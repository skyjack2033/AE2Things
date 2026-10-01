package com.asdflj.ae2thing.common.item;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PhialAmountConverterTest {

    private static final long ESSENTIA_PER_ITEM = 144;

    @Test
    public void reportsExtractionWhenTheCurrentTotalDropsBelowAnItemBoundary() {
        assertEquals(-1, PhialAmountConverter.calculateItemDelta(ESSENTIA_PER_ITEM - 1, -1, ESSENTIA_PER_ITEM));
    }

    @Test
    public void reportsInjectionWhenTheCurrentTotalReachesAnItemBoundary() {
        assertEquals(1, PhialAmountConverter.calculateItemDelta(ESSENTIA_PER_ITEM, 1, ESSENTIA_PER_ITEM));
    }

    @Test
    public void ignoresChangesThatStayWithinTheSameItemUnit() {
        assertEquals(0, PhialAmountConverter.calculateItemDelta(ESSENTIA_PER_ITEM - 2, -1, ESSENTIA_PER_ITEM));
        assertEquals(0, PhialAmountConverter.calculateItemDelta(ESSENTIA_PER_ITEM - 1, 1, ESSENTIA_PER_ITEM));
    }

    @Test
    public void reportsEveryCrossedItemBoundary() {
        assertEquals(-2, PhialAmountConverter.calculateItemDelta(0, -(2 * ESSENTIA_PER_ITEM), ESSENTIA_PER_ITEM));
        assertEquals(
            2,
            PhialAmountConverter.calculateItemDelta(2 * ESSENTIA_PER_ITEM, 2 * ESSENTIA_PER_ITEM, ESSENTIA_PER_ITEM));
    }
}

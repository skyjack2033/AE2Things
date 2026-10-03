package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.BiPredicate;

import org.junit.Test;

public class InterfaceTerminalSearchTest {

    private static final BiPredicate<String, String> TEXT_MATCHER = (query, name) -> name.contains(query);

    @Test
    public void circuitOneDoesNotMatchLongerCircuitNumbers() {
        assertTrue(matches("1", "Circuit 1"));
        assertFalse(matches("1", "Circuit 10"));
        assertFalse(matches("1", "Circuit 11"));
        assertFalse(matches("1", "Circuit 21"));
        assertFalse(matches("10", "Circuit 101"));
    }

    @Test
    public void acceptsGtCircuitListsAndProgrammableHatchSuffixes() {
        assertTrue(matches("Assembler 1", "Assembler [1, 2]"));
        assertTrue(matches("Assembler 2", "Assembler [1, 2]"));
        assertTrue(matches("Assembler 1", "Assembler - 1"));
        assertFalse(matches("Assembler 1", "Assembler - 21"));
    }

    @Test
    public void numericWordsMayBeDelimitedByTextOrPunctuation() {
        assertTrue(matches("1", "机器1号"));
        assertTrue(matches("1", "Circuit(1)"));
        assertTrue(matches("1", "1"));
        assertFalse(matches("1", "机器01号"));
    }

    @Test
    public void searchesLaterOccurrencesAfterRejectingLongerNumbers() {
        assertTrue(matches("1", "Assembler [10, 21, 1]"));
        assertFalse(matches("1", "Assembler [10, 21, 31]"));
    }

    @Test
    public void removesFormattingWithoutCreatingFalseCircuitNumbers() {
        assertFalse(matches("1", "\u00a71Assembler"));
        assertTrue(matches("1", "\u00a76Assembler \u00a7r[\u00a7a1\u00a7r]"));
        assertFalse(matches("1", "Assembler 1\u00a7a0"));
        assertTrue(matches("\u00a71ASSEMBLER 1", "Assembler [1]"));
    }

    @Test
    public void allWhitespaceSeparatedWordsMustMatch() {
        assertTrue(matches("  ASSEMBLER\t1\n2  ", "Assembler [1, 2]"));
        assertFalse(matches("Assembler 1 3", "Assembler [1, 2]"));
        assertFalse(matches("Macerator 1", "Assembler [1]"));
    }

    @Test
    public void emptyQueriesMatchWithoutCallingTheTextMatcher() {
        assertTrue(InterfaceTerminalSearch.matches("", "Assembler", (query, name) -> false));
        assertTrue(InterfaceTerminalSearch.matches(" \t\n ", "", (query, name) -> false));
        assertTrue(InterfaceTerminalSearch.matches("\u00a71\u00a7r", "Assembler", (query, name) -> false));
    }

    @Test
    public void delegatesOrdinaryWordsToTheExistingTextAndPinyinMatcher() {
        BiPredicate<String, String> pinyinMatcher = (query, name) -> query.equals("zhuangpei")
            && name.equals("装配机 [1]");

        assertTrue(InterfaceTerminalSearch.matches("ZHUANGPEI 1", "装配机 [1]", pinyinMatcher));
        assertFalse(InterfaceTerminalSearch.matches("zhuangpei 2", "装配机 [1]", pinyinMatcher));
        assertTrue(matches("MK1", "Assembler MK10"));
    }

    private static boolean matches(String query, String name) {
        return InterfaceTerminalSearch.matches(query, name, TEXT_MATCHER);
    }
}

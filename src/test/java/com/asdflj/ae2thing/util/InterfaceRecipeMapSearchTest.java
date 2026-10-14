package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

import org.junit.Test;

public class InterfaceRecipeMapSearchTest {

    private static final String ASSEMBLER = "gt.recipe.assembler";
    private static final String MACERATOR = "gt.recipe.macerator";
    private static final BiPredicate<String, String> TEXT_MATCHER = (query, name) -> name.contains(query);
    private static final UnaryOperator<String> TRANSLATE_MAP = id -> {
        if (ASSEMBLER.equals(id)) return "Assembler";
        if (MACERATOR.equals(id)) return "Macerator";
        return id;
    };

    private final InterfaceRecipeMapSearch search = new InterfaceRecipeMapSearch();

    @Test
    public void exactRecipeMapMatchesRenamedMachinesWithoutMatchingTheirName() {
        search.setRecipe(ASSEMBLER, "Assembler", "");

        assertTrue(matches("Assembler", "My Production Line", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler", "Assembler", maps(MACERATOR), true));
        assertFalse(matches("Assembler", "Assembler", maps(ASSEMBLER + ".other"), true));
    }

    @Test
    public void identicalDisplayNamesAreFilteredByEachInterfacesRecipeMap() {
        search.setRecipe(ASSEMBLER, "Assembler", "");

        assertTrue(matches("Assembler", "Shared Name", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler", "Shared Name", maps(MACERATOR), true));
    }

    @Test
    public void exactRecipeMapStillRequiresAllExtraTermsAndCompleteCircuitNumbers() {
        search.setRecipe(ASSEMBLER, "Assembler 1 Lens", "1 Lens");

        assertTrue(matches("Assembler 1 Lens", "Custom Line [1] Lens", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler 1 Lens", "Custom Line [10] Lens", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler 1 Lens", "Custom Line [1]", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler 1 Lens", "Custom Line [1] Lens", maps(MACERATOR), true));
    }

    @Test
    public void unknownRecipeMapsFallBackToTheOriginalNameSearch() {
        search.setRecipe(ASSEMBLER, "Assembler 1", "1");

        assertTrue(matches("Assembler 1", "Assembler [1]", Collections.emptySet(), true));
        assertTrue(matches("Assembler 1", "Assembler [1]", null, true));
        assertFalse(matches("Assembler 1", "Renamed [1]", Collections.emptySet(), true));
        assertFalse(matches("Assembler 1", "Assembler [10]", null, true));
    }

    @Test
    public void disablingRecipeMapMatchingUsesOnlyTheOriginalName() {
        search.setRecipe(ASSEMBLER, "Assembler", "");

        assertFalse(matches("Assembler", "Renamed Machine", maps(ASSEMBLER), false));
        assertTrue(matches("Assembler", "Assembler", maps(MACERATOR), false));
    }

    @Test
    public void manuallyChangedQueriesSearchTranslatedRecipeMapNames() {
        search.setRecipe(MACERATOR, "Macerator", "");

        assertTrue(matches("Assembler 1", "Renamed Machine [1]", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler 1", "Renamed Machine [10]", maps(ASSEMBLER), true));
        assertFalse(matches("Assembler 1", "Renamed Machine [1]", maps(MACERATOR), true));
        assertTrue(matches("Renamed 1", "Renamed Machine [1]", maps(ASSEMBLER), true));
    }

    @Test
    public void manualSearchIncludesEveryTranslatedRecipeMapOnMultimodeMachines() {
        assertTrue(matches("Assembler", "Renamed Machine", maps(ASSEMBLER, MACERATOR), true));
        assertTrue(matches("Macerator", "Renamed Machine", maps(ASSEMBLER, MACERATOR), true));
        assertFalse(matches("Assembler", "Renamed Machine", maps(MACERATOR), true));
    }

    @Test
    public void clearingTheRecipeRestoresManualTranslatedNameSearch() {
        search.setRecipe(ASSEMBLER, "Assembler", "");
        search.clear();

        assertTrue(matches("Assembler", "Assembler", maps(MACERATOR), true));
        assertTrue(matches("Assembler", "Renamed Machine", maps(ASSEMBLER), true));
    }

    private boolean matches(String query, String displayName, Set<String> recipeMaps, boolean matchRecipeMaps) {
        return search.matches(query, displayName, recipeMaps, matchRecipeMaps, TRANSLATE_MAP, TEXT_MATCHER);
    }

    private static Set<String> maps(String... ids) {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, ids);
        return result;
    }
}

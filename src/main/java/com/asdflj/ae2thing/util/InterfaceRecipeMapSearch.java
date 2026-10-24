package com.asdflj.ae2thing.util;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

/** Keeps NEI's exact recipe identity separate from the editable search text. */
public final class InterfaceRecipeMapSearch {

    private static final String FAKE_CUTTING_SAW = "gt.recipe.fakecuttingsaw";
    private static final String CUTTING_SAW = "gt.recipe.cuttingsaw";
    private static final String MULTI_CENTRIFUGE = "gt.recipe.multicentrifuge";
    private static final String CENTRIFUGE = "gt.recipe.centrifuge";
    private static final String MULTI_ELECTROLYZER = "gt.recipe.multielectro";
    private static final String ELECTROLYZER = "gt.recipe.electrolyzer";
    private static final String MULTI_MIXER = "gt.recipe.multimixer";
    private static final String MIXER = "gt.recipe.mixer";
    private static final String MULTI_DEHYDRATOR = "gt.recipe.multidehydrator";
    private static final String DEHYDRATOR = "gt.recipe.chemicaldehydrator";

    private String recipeMapId = "";
    private String recipeQuery = "";
    private String extraTerms = "";

    public void setRecipe(String recipeMapId, String query, String extraTerms) {
        this.recipeMapId = recipeMapId == null ? "" : recipeMapId;
        this.recipeQuery = query;
        this.extraTerms = extraTerms;
    }

    public void clear() {
        recipeMapId = "";
        recipeQuery = "";
        extraTerms = "";
    }

    public boolean matches(String query, String displayName, Set<String> recipeMaps, boolean matchRecipeMaps,
        UnaryOperator<String> translateRecipeMap, BiPredicate<String, String> textMatcher) {
        if (!matchRecipeMaps || recipeMaps == null || recipeMaps.isEmpty()) {
            return InterfaceTerminalSearch.matches(query, displayName, textMatcher);
        }
        if (!recipeMapId.isEmpty() && recipeQuery.equals(query)) {
            return containsRecipeMap(recipeMaps, recipeMapId)
                && InterfaceTerminalSearch.matches(extraTerms, displayName, textMatcher);
        }
        StringBuilder names = new StringBuilder(displayName);
        for (String map : recipeMaps) {
            names.append(' ')
                .append(translateRecipeMap.apply(map))
                .append(' ')
                .append(map);
            String canonicalMap = canonicalMap(map);
            if (!canonicalMap.equals(map)) {
                names.append(' ')
                    .append(translateRecipeMap.apply(canonicalMap))
                    .append(' ')
                    .append(canonicalMap);
            }
        }
        return InterfaceTerminalSearch.matches(query, names.toString(), textMatcher);
    }

    private static boolean containsRecipeMap(Set<String> recipeMaps, String recipeMapId) {
        String canonicalRecipeMap = canonicalMap(recipeMapId);
        for (String map : recipeMaps) {
            if (canonicalRecipeMap.equals(canonicalMap(map))) return true;
        }
        return false;
    }

    private static String canonicalMap(String map) {
        if (FAKE_CUTTING_SAW.equals(map)) return CUTTING_SAW;
        if (MULTI_CENTRIFUGE.equals(map)) return CENTRIFUGE;
        if (MULTI_ELECTROLYZER.equals(map)) return ELECTROLYZER;
        if (MULTI_MIXER.equals(map)) return MIXER;
        if (MULTI_DEHYDRATOR.equals(map)) return DEHYDRATOR;
        return map;
    }
}

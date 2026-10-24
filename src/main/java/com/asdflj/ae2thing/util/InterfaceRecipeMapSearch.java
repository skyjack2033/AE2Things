package com.asdflj.ae2thing.util;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

/** Keeps NEI's exact recipe identity separate from the editable search text. */
public final class InterfaceRecipeMapSearch {

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
            return recipeMaps.contains(recipeMapId)
                && InterfaceTerminalSearch.matches(extraTerms, displayName, textMatcher);
        }
        StringBuilder names = new StringBuilder(displayName);
        for (String map : recipeMaps) {
            names.append(' ')
                .append(translateRecipeMap.apply(map))
                .append(' ')
                .append(map);
        }
        return InterfaceTerminalSearch.matches(query, names.toString(), textMatcher);
    }
}

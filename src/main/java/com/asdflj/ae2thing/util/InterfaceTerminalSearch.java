package com.asdflj.ae2thing.util;

import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/** Name search shared by machine names and their circuit suffixes. */
public final class InterfaceTerminalSearch {

    private static final Pattern FORMATTING = Pattern.compile("\u00a7[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private InterfaceTerminalSearch() {}

    /** The text matcher receives the search word first, followed by the normalized machine name. */
    public static boolean matches(String query, String name, BiPredicate<String, String> textMatcher) {
        String normalizedQuery = normalize(query).trim();
        if (normalizedQuery.isEmpty()) return true;

        String normalizedName = normalize(name);
        for (String word : WHITESPACE.split(normalizedQuery)) {
            if (word.codePoints()
                .allMatch(Character::isDigit)) {
                if (!containsNumber(normalizedName, word)) return false;
            } else if (!textMatcher.test(word, normalizedName)) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String text) {
        return FORMATTING.matcher(text)
            .replaceAll("")
            .toLowerCase(Locale.ROOT);
    }

    private static boolean containsNumber(String name, String number) {
        int start = name.indexOf(number);
        while (start >= 0) {
            int end = start + number.length();
            if ((start == 0 || !Character.isDigit(name.codePointBefore(start)))
                && (end == name.length() || !Character.isDigit(name.codePointAt(end)))) {
                return true;
            }
            start = name.indexOf(number, start + 1);
        }
        return false;
    }
}

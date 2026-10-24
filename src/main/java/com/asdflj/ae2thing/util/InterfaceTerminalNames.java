package com.asdflj.ae2thing.util;

import java.util.function.UnaryOperator;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import com.asdflj.ae2thing.integration.Mods;

import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Resolves both AE2's split names and the combined names used by older interface providers. */
public final class InterfaceTerminalNames {

    private InterfaceTerminalNames() {}

    public static String getDisplayName(String rawName, String suffix, ItemStack... icons) {
        return getDisplayName(rawName, suffix, key -> {
            for (ItemStack icon : icons) {
                if (icon != null && key.equals(icon.getUnlocalizedName())) {
                    return icon.getDisplayName();
                }
            }
            String translated = CraftingCPUCluster.translateFromNetwork(key);
            if (!key.equals(translated)) return translated;
            if (Mods.isGt5UnofficialLoaded() || Mods.isLegacyGt5Loaded()) {
                return GTMachineNames.getDisplayName(key);
            }
            return key;
        });
    }

    static String getDisplayName(String rawName, String suffix, UnaryOperator<String> translate) {
        String name = translateName(rawName == null ? "" : rawName, translate);
        if (suffix == null || suffix.isEmpty()) return name;
        try {
            IChatComponent component = IChatComponent.Serializer.func_150699_a(suffix);
            return name + (component == null ? suffix : component.getUnformattedText());
        } catch (Exception ignored) {
            return name + suffix;
        }
    }

    private static String translateName(String rawName, UnaryOperator<String> translate) {
        String translated = translate.apply(rawName);
        if (!rawName.equals(translated)) return translated;

        // PH's legacy getName() can append a circuit/item suffix before the name reaches the client.
        // Translate only a recognized base name; arbitrary custom names must remain untouched.
        int suffixStart = rawName.length();
        for (String separator : new String[] { " - ", " [", " {" }) {
            int index = rawName.indexOf(separator);
            if (index > 0) suffixStart = Math.min(suffixStart, index);
        }
        if (suffixStart < rawName.length()) {
            String base = rawName.substring(0, suffixStart);
            String translatedBase = translate.apply(base);
            if (!base.equals(translatedBase)) return translatedBase + rawName.substring(suffixStart);
        }
        return rawName;
    }
}

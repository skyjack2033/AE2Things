package com.asdflj.ae2thing.nei;

import java.util.ArrayList;
import java.util.List;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.Tags;
import com.asdflj.ae2thing.client.gui.GuiCraftingTerminal;
import com.asdflj.ae2thing.client.gui.GuiInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.common.Config;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.recipes.FluidRecipe;
import com.asdflj.ae2thing.nei.recipes.extractor.GTRecipeExtractor;

import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

@SuppressWarnings("unused")
public class NEI_TH_Config implements IConfigureNEI {

    public static final String APPEND_NON_CONSUMED_ITEMS = "ae2thing.dual_interface_terminal_append_non_consumed_items";
    public static final String MATCH_RECIPE_MAPS = "ae2thing.dual_interface_terminal_match_recipe_maps";
    private static final ConfigTagParent tag = NEIClientConfig.global.config;

    @Override
    public void loadConfig() {
        Config.migrateInterfaceTerminalSettings(
            tag.getTag(ButtonConstants.DUAL_INTERFACE_TERMINAL)
                .getBooleanValue(false),
            tag.getTag(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)
                .getBooleanValue(true),
            Mods.PROGRAMMABLE_HATCHES.isModLoaded() && tag.getTag(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT)
                .getBooleanValue(false));
        API.registerNEIGuiHandler(new AE2TH_NEIGuiHandler());
        List<String> recipes = new ArrayList<>();
        recipes.add("crafting");
        recipes.add("crafting2x2");
        for (String identifier : recipes) {
            // that NEE handlers take priority
            if (!API.hasGuiOverlayHandler(GuiCraftingTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(GuiCraftingTerminal.class, CraftingTransferHandler.INSTANCE, identifier);
            }
        }
        recipes.clear();
        recipes.add("infusionCrafting");
        recipes.add("cruciblerecipe");
        for (String identifier : recipes) {
            if (!API.hasGuiOverlayHandler(GuiInfusionPatternTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiInfusionPatternTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        // NEI loads plugins after FML load-complete, when addon recipe categories are also available.
        if (Mods.isGt5UnofficialLoaded()) {
            GTRecipeExtractor.registerRecipeMaps();
        }
        for (String identifier : FluidRecipe.getSupportRecipes()) {
            if (!API.hasGuiOverlayHandler(GuiWirelessDualInterfaceTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiWirelessDualInterfaceTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        API.addOption(new BaseToggleButton(ButtonConstants.HISTORY, false));
        API.addOption(new BaseToggleButton(ButtonConstants.INVENTORY_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.ULTRA_TERMINAL_MODE));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL, false));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE));
        API.addOption(new BaseToggleButton(APPEND_NON_CONSUMED_ITEMS));
        API.addOption(new BaseToggleButton(MATCH_RECIPE_MAPS));
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR)); //remove
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR_REMOVE));
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR_CRAFTING_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.CRAFTING_NOTIFICATION));
        API.addOption(new BaseToggleButton(ButtonConstants.NEI_CRAFT_ITEM));
        if (Mods.BLOCK_RENDERER.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.BLOCK_RENDER));
        }
        API.registerBookmarkContainerHandler(
            GuiWirelessDualInterfaceTerminal.class,
            DualInterfaceBookmarkContainerHandler.INSTANCE);
    }

    public static boolean getConfigValue(String identifier) {
        switch (identifier) {
            case ButtonConstants.DUAL_INTERFACE_TERMINAL:
                return Config.interfaceTerminalAutoFillSearchNames;
            case ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE:
                return Config.interfaceTerminalAppendCircuitNumbers;
            case APPEND_NON_CONSUMED_ITEMS:
                return Config.interfaceTerminalAppendNonConsumedItems;
            case MATCH_RECIPE_MAPS:
                return Config.interfaceTerminalMatchRecipeMaps;
            case ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT:
                return !Config.interfaceTerminalAppendCircuitNumbers && !Config.interfaceTerminalAppendNonConsumedItems;
            default:
                break;
        }
        return tag.getTag(identifier)
            .getBooleanValue(true);
    }

    public static boolean isInterfaceTerminalSearchOption(String identifier) {
        return identifier.equals(ButtonConstants.DUAL_INTERFACE_TERMINAL)
            || identifier.equals(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)
            || identifier.equals(APPEND_NON_CONSUMED_ITEMS)
            || identifier.equals(MATCH_RECIPE_MAPS);
    }

    public static void setInterfaceTerminalSearchOption(String identifier, boolean value) {
        switch (identifier) {
            case ButtonConstants.DUAL_INTERFACE_TERMINAL:
                Config.interfaceTerminalAutoFillSearchNames = value;
                break;
            case ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE:
                Config.interfaceTerminalAppendCircuitNumbers = value;
                break;
            case APPEND_NON_CONSUMED_ITEMS:
                Config.interfaceTerminalAppendNonConsumedItems = value;
                break;
            case MATCH_RECIPE_MAPS:
                Config.interfaceTerminalMatchRecipeMaps = value;
                break;
            default:
                return;
        }
        Config.saveInterfaceTerminalSettings();
    }

    @Override
    public String getName() {
        return AE2Thing.NAME;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}

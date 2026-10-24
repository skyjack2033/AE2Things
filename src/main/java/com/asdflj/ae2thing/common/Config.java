package com.asdflj.ae2thing.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.asdflj.ae2thing.AE2Thing;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class Config {

    public static final String CATEGORY_GENERAL = AE2Thing.NAME.toLowerCase(Locale.ENGLISH);
    public static final String CATEGORY_INTERFACE_TERMINAL_SEARCH = "interface_terminal_search";
    private static final String AUTO_FILL_SEARCH_NAMES = "auto_fill";
    private static final String APPEND_CIRCUIT_NUMBERS = "append_circuit_numbers";
    private static final String APPEND_NON_CONSUMED_ITEMS = "append_non_consumed_items";
    private static final String MATCH_RECIPE_MAPS = "match_recipe_maps";
    private static final Configuration Config = new Configuration(
        new File(new File((File) FMLInjectionData.data()[6], "config"), AE2Thing.MODID + ".cfg"));
    private static boolean migrateAutoFillSearchNames;
    private static boolean migrateAppendCircuitNumbers;
    private static boolean migrateAppendNonConsumedItems;
    public static boolean cellLink;
    public static int magnetRange;
    public static boolean updateViewThread = true;
    public static boolean wirelessConnectorTerminalInfinityConnectionRange = true;
    public static int exIOPortTransferContentsRate;
    public static boolean backpackTerminalAddTicSupport = false;
    public static int craftingHistorySize = 200;
    public static boolean interfaceTerminalAutoFillSearchNames = false;
    public static boolean interfaceTerminalAppendCircuitNumbers = true;
    public static boolean interfaceTerminalAppendNonConsumedItems = true;
    public static boolean interfaceTerminalMatchRecipeMaps = true;

    public static void run() {
        migrateAutoFillSearchNames = !Config.hasKey(CATEGORY_INTERFACE_TERMINAL_SEARCH, AUTO_FILL_SEARCH_NAMES);
        migrateAppendCircuitNumbers = !Config.hasKey(CATEGORY_INTERFACE_TERMINAL_SEARCH, APPEND_CIRCUIT_NUMBERS);
        migrateAppendNonConsumedItems = !Config.hasKey(CATEGORY_INTERFACE_TERMINAL_SEARCH, APPEND_NON_CONSUMED_ITEMS);
        loadCategory();
        loadProperty();
        // Leave new client options unsaved until NEI has supplied the player's legacy preferences.
        loadInterfaceTerminalSettings();
    }

    public static Configuration getConfiguration() {
        return Config;
    }

    private static void loadProperty() {
        cellLink = Config
            .getBoolean("Enable link cell", AE2Thing.NAME, true, "Enable link Cell,It will link every same uuid cell");
        magnetRange = Config
            .getInt("Backpack terminal magnet range", AE2Thing.NAME, 32, 8, 64, "Set backpack terminal magnet range");
        updateViewThread = Config
            .getBoolean("Terminal updateView thread", AE2Thing.NAME, true, "Terminal create update view thread");
        wirelessConnectorTerminalInfinityConnectionRange = Config.getBoolean(
            "Wireless connector terminal infinity connection range",
            AE2Thing.NAME,
            true,
            "Wireless connector terminal infinity connection range");
        exIOPortTransferContentsRate = Config.getInt(
            "Ex IO Port Transfer Rate",
            AE2Thing.NAME,
            256,
            256,
            Integer.MAX_VALUE,
            "Set Ex IO Port Transfer Rate. Base transfer quantity = 256. Make sure you have enough power to transfer stack.");
        backpackTerminalAddTicSupport = Config
            .getBoolean("Tic", AE2Thing.NAME, false, "Let Backpack Terminal can forge tic tool");
        craftingHistorySize = Config
            .getInt("crafting history size", AE2Thing.NAME, 200, 100, 300, "crafting history size");
        Config.getCategory(CATEGORY_GENERAL)
            .get("Enable link cell")
            .setLanguageKey("ae2thing.config.general.cell_link");
        Config.getCategory(CATEGORY_GENERAL)
            .get("Backpack terminal magnet range")
            .setLanguageKey("ae2thing.config.general.magnet_range");
        Config.getCategory(CATEGORY_GENERAL)
            .get("Terminal updateView thread")
            .setLanguageKey("ae2thing.config.general.update_view_thread");
        Config.getCategory(CATEGORY_GENERAL)
            .get("Wireless connector terminal infinity connection range")
            .setLanguageKey("ae2thing.config.general.infinity_connection_range");
        Config.getCategory(CATEGORY_GENERAL)
            .get("Ex IO Port Transfer Rate")
            .setLanguageKey("ae2thing.config.general.ex_io_port_transfer_rate");
        Config.getCategory(CATEGORY_GENERAL)
            .get("Tic")
            .setLanguageKey("ae2thing.config.general.tic_support");
        Config.getCategory(CATEGORY_GENERAL)
            .get("crafting history size")
            .setLanguageKey("ae2thing.config.general.crafting_history_size");
        if (Config.hasChanged()) Config.save();
    }

    private static void loadCategory() {
        Config.addCustomCategoryComment(AE2Thing.NAME, "Settings for AE2Thing.");
        Config.getCategory(CATEGORY_GENERAL)
            .setLanguageKey("ae2thing.config.general")
            .setRequiresMcRestart(true);
        Config.addCustomCategoryComment(
            CATEGORY_INTERFACE_TERMINAL_SEARCH,
            "Client-side interface terminal recipe search settings. Changes apply immediately.");
        Config.getCategory(CATEGORY_INTERFACE_TERMINAL_SEARCH)
            .setLanguageKey("ae2thing.config.interface_terminal_search")
            .setPropertyOrder(
                new ArrayList<>(
                    Arrays.asList(
                        AUTO_FILL_SEARCH_NAMES,
                        APPEND_CIRCUIT_NUMBERS,
                        APPEND_NON_CONSUMED_ITEMS,
                        MATCH_RECIPE_MAPS)));
    }

    private static Property searchProperty(String name, boolean defaultValue, String comment) {
        return Config.get(CATEGORY_INTERFACE_TERMINAL_SEARCH, name, defaultValue, comment)
            .setLanguageKey("ae2thing.config.interface_terminal_search." + name)
            .setRequiresMcRestart(false);
    }

    private static void loadInterfaceTerminalSettings() {
        interfaceTerminalAutoFillSearchNames = searchProperty(
            AUTO_FILL_SEARCH_NAMES,
            false,
            "Automatically filter interfaces when transferring an NEI recipe.").getBoolean();
        interfaceTerminalAppendCircuitNumbers = searchProperty(
            APPEND_CIRCUIT_NUMBERS,
            true,
            "Include integrated circuit numbers when searching for an interface.").getBoolean();
        interfaceTerminalAppendNonConsumedItems = searchProperty(
            APPEND_NON_CONSUMED_ITEMS,
            true,
            "Include non-consumed ingredients, such as molds, when searching for an interface.").getBoolean();
        interfaceTerminalMatchRecipeMaps = searchProperty(
            MATCH_RECIPE_MAPS,
            true,
            "Match interfaces by the recipe map of their machine; fall back to names when unavailable.").getBoolean();
    }

    public static void migrateInterfaceTerminalSettings(boolean autoFill, boolean appendItems, boolean ignoreCircuit) {
        if (migrateAutoFillSearchNames) interfaceTerminalAutoFillSearchNames = autoFill;
        if (migrateAppendCircuitNumbers) interfaceTerminalAppendCircuitNumbers = appendItems && !ignoreCircuit;
        if (migrateAppendNonConsumedItems) interfaceTerminalAppendNonConsumedItems = appendItems && !ignoreCircuit;
        saveInterfaceTerminalSettings();
    }

    public static void syncInterfaceTerminalSettings() {
        loadInterfaceTerminalSettings();
        saveInterfaceTerminalSettings();
    }

    public static void saveInterfaceTerminalSettings() {
        Config.getCategory(CATEGORY_INTERFACE_TERMINAL_SEARCH)
            .get(AUTO_FILL_SEARCH_NAMES)
            .set(interfaceTerminalAutoFillSearchNames);
        Config.getCategory(CATEGORY_INTERFACE_TERMINAL_SEARCH)
            .get(APPEND_CIRCUIT_NUMBERS)
            .set(interfaceTerminalAppendCircuitNumbers);
        Config.getCategory(CATEGORY_INTERFACE_TERMINAL_SEARCH)
            .get(APPEND_NON_CONSUMED_ITEMS)
            .set(interfaceTerminalAppendNonConsumedItems);
        Config.getCategory(CATEGORY_INTERFACE_TERMINAL_SEARCH)
            .get(MATCH_RECIPE_MAPS)
            .set(interfaceTerminalMatchRecipeMaps);
        migrateAutoFillSearchNames = false;
        migrateAppendCircuitNumbers = false;
        migrateAppendNonConsumedItems = false;
        if (Config.hasChanged()) Config.save();
    }
}

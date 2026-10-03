package com.asdflj.ae2thing.util;

import java.util.EnumSet;

import appeng.api.config.Settings;
import appeng.api.config.ViewItems;
import appeng.api.util.IConfigManager;

/** Custom terminals do not receive the flow-rate updates required by AE2's FLOWING view. */
public final class TerminalViewMode {

    private TerminalViewMode() {}

    public static EnumSet<?> getPossibleValues(Settings setting) {
        EnumSet<?> values = setting.getPossibleValues()
            .clone();
        if (setting == Settings.VIEW_MODE) {
            values.remove(ViewItems.FLOWING);
        }
        return values;
    }

    public static void normalize(IConfigManager manager) {
        if (manager != null && manager.getSetting(Settings.VIEW_MODE) == ViewItems.FLOWING) {
            manager.putSetting(Settings.VIEW_MODE, ViewItems.ALL);
        }
    }
}

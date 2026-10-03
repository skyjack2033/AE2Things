package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.ViewItems;
import appeng.util.ConfigManager;

public class TerminalViewModeTest {

    @Test
    public void customTerminalChoicesExcludeFlowingWithoutChangingAe2Choices() {
        EnumSet<?> choices = TerminalViewMode.getPossibleValues(Settings.VIEW_MODE);

        assertFalse(choices.contains(ViewItems.FLOWING));
        for (ViewItems mode : ViewItems.values()) {
            if (mode != ViewItems.FLOWING) {
                assertTrue(choices.contains(mode));
            }
        }
        assertTrue(
            Settings.VIEW_MODE.getPossibleValues()
                .contains(ViewItems.FLOWING));
        assertEquals(
            Settings.SORT_DIRECTION.getPossibleValues(),
            TerminalViewMode.getPossibleValues(Settings.SORT_DIRECTION));
    }

    @Test
    public void savedFlowingModeBecomesAllWhileSupportedModesAndSortDirectionSurvive() {
        for (ViewItems mode : ViewItems.values()) {
            NBTTagCompound saved = new NBTTagCompound();
            saved.setString(Settings.VIEW_MODE.name(), mode.name());
            saved.setString(Settings.SORT_DIRECTION.name(), SortDir.DESCENDING.name());

            ConfigManager manager = new ConfigManager((config, setting, value) -> {});
            manager.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
            manager.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
            manager.readFromNBT(saved);

            TerminalViewMode.normalize(manager);
            manager.writeToNBT(saved);

            ViewItems expected = mode == ViewItems.FLOWING ? ViewItems.ALL : mode;
            assertEquals(expected, manager.getSetting(Settings.VIEW_MODE));
            assertEquals(expected.name(), saved.getString(Settings.VIEW_MODE.name()));
            assertEquals(SortDir.DESCENDING.name(), saved.getString(Settings.SORT_DIRECTION.name()));
        }
    }
}

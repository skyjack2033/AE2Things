package com.asdflj.ae2thing.client.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.Config;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiAE2ThingConfig extends GuiConfig {

    public GuiAE2ThingConfig(GuiScreen parent) {
        super(
            parent,
            getConfigElements(),
            AE2Thing.MODID,
            false,
            false,
            I18n.format("ae2thing.config.title"),
            I18n.format("ae2thing.config.interface_terminal_search"));
    }

    @SuppressWarnings("rawtypes")
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<>(
            new ConfigElement<>(
                Config.getConfiguration()
                    .getCategory(Config.CATEGORY_INTERFACE_TERMINAL_SEARCH)).getChildElements());
        elements.add(
            new ConfigElement<>(
                Config.getConfiguration()
                    .getCategory(Config.CATEGORY_GENERAL)));
        return elements;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
        if (button.id == 2000) {
            Config.syncInterfaceTerminalSettings();
        }
    }
}

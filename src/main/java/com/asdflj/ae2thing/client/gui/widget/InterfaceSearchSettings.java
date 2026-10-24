package com.asdflj.ae2thing.client.gui.widget;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import org.lwjgl.opengl.GL11;

import com.asdflj.ae2thing.common.Config;

import cpw.mods.fml.client.config.GuiCheckBox;

/** An in-place settings dialog keeps the terminal's server container open. */
public final class InterfaceSearchSettings extends Gui {

    private static final String PREFIX = "ae2thing.config.interface_terminal_search.";
    private static final String[] OPTIONS = { "auto_fill", "append_circuit_numbers", "append_non_consumed_items",
        "match_recipe_maps" };
    private final List<GuiCheckBox> toggles = new ArrayList<>();
    private final Runnable onChange;
    private final Runnable onClose;
    private final GuiButton done;
    private final int left;
    private final int top;
    private final int width;
    private final int height = 140;

    public InterfaceSearchSettings(int screenWidth, int screenHeight, Runnable onChange, Runnable onClose) {
        Minecraft mc = Minecraft.getMinecraft();
        this.onChange = onChange;
        this.onClose = onClose;
        int labelWidth = mc.fontRenderer.getStringWidth(I18n.format("ae2thing.gui.search_settings"));
        for (String option : OPTIONS) {
            labelWidth = Math.max(labelWidth, mc.fontRenderer.getStringWidth(I18n.format(PREFIX + option)));
        }
        width = Math.min(screenWidth - 16, Math.max(220, labelWidth + 40));
        left = (screenWidth - width) / 2;
        top = (screenHeight - height) / 2;
        boolean[] values = { Config.interfaceTerminalAutoFillSearchNames, Config.interfaceTerminalAppendCircuitNumbers,
            Config.interfaceTerminalAppendNonConsumedItems, Config.interfaceTerminalMatchRecipeMaps };
        for (int i = 0; i < OPTIONS.length; i++) {
            String label = mc.fontRenderer.trimStringToWidth(I18n.format(PREFIX + OPTIONS[i]), width - 40);
            toggles.add(new GuiCheckBox(i, left + 12, top + 30 + i * 19, label, values[i]));
        }
        done = new GuiButton(4, left + (width - 100) / 2, top + height - 30, 100, 20, I18n.format("gui.done"));
    }

    public void draw(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 600);
        drawRect(0, 0, screenWidth, screenHeight, 0x99000000);
        drawRect(left - 1, top - 1, left + width + 1, top + height + 1, 0xffa0a0a0);
        drawRect(left, top, left + width, top + height, 0xff303030);
        drawCenteredString(
            mc.fontRenderer,
            I18n.format("ae2thing.gui.search_settings"),
            left + width / 2,
            top + 10,
            0xffffff);
        for (GuiCheckBox toggle : toggles) {
            toggle.drawButton(mc, mouseX, mouseY);
        }
        done.drawButton(mc, mouseX, mouseY);
        GL11.glPopMatrix();
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        for (GuiCheckBox toggle : toggles) {
            if (!toggle.mousePressed(mc, mouseX, mouseY)) continue;
            toggle.func_146113_a(mc.getSoundHandler());
            Config.interfaceTerminalAutoFillSearchNames = toggles.get(0)
                .isChecked();
            Config.interfaceTerminalAppendCircuitNumbers = toggles.get(1)
                .isChecked();
            Config.interfaceTerminalAppendNonConsumedItems = toggles.get(2)
                .isChecked();
            Config.interfaceTerminalMatchRecipeMaps = toggles.get(3)
                .isChecked();
            Config.saveInterfaceTerminalSettings();
            onChange.run();
            return;
        }
        if (done.mousePressed(mc, mouseX, mouseY)) {
            done.func_146113_a(mc.getSoundHandler());
            onClose.run();
        }
    }
}

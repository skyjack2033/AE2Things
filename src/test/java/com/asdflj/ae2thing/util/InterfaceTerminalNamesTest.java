package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.function.UnaryOperator;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

public class InterfaceTerminalNamesTest {

    private static final String ASSEMBLY_LINE = "gt.blockmachines.multimachine.assemblyline";
    private static final Map<String, String> NAMES = Map.of(ASSEMBLY_LINE, "装配线");
    private static final UnaryOperator<String> TRANSLATE = key -> NAMES.getOrDefault(key, key);

    @Test
    public void resolvesAnIconsDynamicDisplayNameFromItsExactRawKey() {
        ItemStack icon = new ItemStack(new Item() {

            @Override
            public String getUnlocalizedName(ItemStack stack) {
                return ASSEMBLY_LINE;
            }

            @Override
            public String getItemStackDisplayName(ItemStack stack) {
                return "装配线";
            }
        });
        assertEquals(
            "装配线 [4]",
            InterfaceTerminalNames.getDisplayName(ASSEMBLY_LINE, "{\"text\":\" [4]\"}", null, icon));
    }

    @Test
    public void translatesMachineBeforeAppendingTheSeparateCircuitComponent() {
        assertEquals("装配线 [4]", display(ASSEMBLY_LINE, "{\"text\":\" [4]\"}"));
    }

    @Test
    public void translatesLegacyCombinedNamesWithoutDroppingAnySuffix() {
        assertEquals("装配线 - 4 - 模具@12", display(ASSEMBLY_LINE + " - 4 - 模具@12", null));
        assertEquals("装配线 [4, 12] {模具}", display(ASSEMBLY_LINE + " [4, 12] {模具}", null));
        assertEquals("装配线 {模具}", display(ASSEMBLY_LINE + " {模具}", null));
    }

    @Test
    public void preservesCustomAndAlreadyTranslatedNames() {
        for (String name : new String[] { "我的装配线 - 4", "装配线 [4]", "factory:Assembly Line", "未知机器" }) {
            assertEquals(name, display(name, null));
        }
    }

    @Test
    public void aTranslationOfTheCompleteNameTakesPrecedenceOverLegacySplitting() {
        assertEquals(
            "完整名称",
            InterfaceTerminalNames.getDisplayName(
                ASSEMBLY_LINE + " - special",
                null,
                key -> key.equals(ASSEMBLY_LINE + " - special") ? "完整名称" : TRANSLATE.apply(key)));
    }

    @Test
    public void acceptsMissingNamesAndLegacyPlainTextSuffixes() {
        assertEquals("", display(null, null));
        assertEquals("装配线 - 7", display(ASSEMBLY_LINE, " - 7"));
        assertEquals("装配线 {invalid", display(ASSEMBLY_LINE, " {invalid"));
    }

    private static String display(String name, String suffix) {
        return InterfaceTerminalNames.getDisplayName(name, suffix, TRANSLATE);
    }
}

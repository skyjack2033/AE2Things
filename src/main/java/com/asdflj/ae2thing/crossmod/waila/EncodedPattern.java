package com.asdflj.ae2thing.crossmod.waila;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import com.asdflj.ae2thing.util.NameConst;
import com.asdflj.ae2thing.util.Util;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.core.localization.GuiText;
import appeng.helpers.PatternHelper;
import appeng.helpers.UltimatePatternHelper;
import appeng.items.misc.ItemEncodedPattern;

public class EncodedPattern extends mcp.mobius.waila.handlers.nei.TooltipHandlerWaila {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack stack, int x, int y, List<String> lines) {
        if (stack != null && stack.getItem() != null && stack.getItem() instanceof ItemEncodedPattern pattern) {
            String lastLine = lines.get(lines.size() - 1);
            String firstLine = lines.get(0);
            lines.clear();
            lines.add(firstLine);
            final EntityPlayer player = mc.thePlayer;
            final NBTTagCompound encodedValue = stack.getTagCompound();

            if (encodedValue == null) {
                lines.add(EnumChatFormatting.RED + GuiText.InvalidPattern.getLocal());
                lines.add(lastLine);
                return lines;
            }
            // Older client validation could persist InvalidPattern=true on an otherwise
            // valid native ultimate pattern. Parse a copy with only that stale cache flag
            // removed; never mutate the stack stored in the player's inventory/network.
            final ItemStack parseStack = stack.copy();
            if (parseStack.stackTagCompound != null) {
                parseStack.stackTagCompound.removeTag("InvalidPattern");
            }
            final ICraftingPatternDetails details = player == null || player.worldObj == null ? null
                : pattern.getPatternForItem(parseStack, player.worldObj);
            final boolean substitute = encodedValue.getBoolean("substitute");
            final boolean beSubstitute = encodedValue.getBoolean("beSubstitute");
            final String author = encodedValue.getString("author");
            final boolean isCrafting = encodedValue.getBoolean("crafting");
            IAEStack<?>[] inItems;
            IAEStack<?>[] outItems;

            if (details == null) {
                final ItemStack unknownItem = new ItemStack(Blocks.fire);
                unknownItem.setStackDisplayName(GuiText.UnknownItem.getLocal());
                // The old fallback only understood ItemStack NBT. Modern ultimate patterns
                // use StackType=fluid/item and are still perfectly displayable even when the
                // world is unavailable or another handler rejected the pattern. Decode both
                // formats here so a valid fluid recipe is never reported as an invalid item.
                inItems = PatternHelper.convertToCondensedAEList(
                    UltimatePatternHelper.loadIAEStackFromNBT(encodedValue.getTagList("in", 10), false, unknownItem));
                outItems = PatternHelper.convertToCondensedAEList(
                    UltimatePatternHelper.loadIAEStackFromNBT(encodedValue.getTagList("out", 10), false, unknownItem));
            } else {
                inItems = details.getCondensedAEInputs();
                outItems = details.getCondensedAEOutputs();
            }

            // When a world is available, respect the item's own validation. Decodable NBT
            // alone does not prove that a crafting recipe is still registered and valid.
            boolean recipeIsBroken = player != null && player.worldObj != null ? details == null
                : inItems.length == 0 || outItems.length == 0;
            final List<String> in = new ArrayList<>();
            final List<String> out = new ArrayList<>();

            final String substitutionLabel = EnumChatFormatting.YELLOW + GuiText.Substitute.getLocal()
                + " "
                + EnumChatFormatting.RESET;
            final String beSubstitutionLabel = EnumChatFormatting.YELLOW + GuiText.BeSubstitute.getLocal()
                + " "
                + EnumChatFormatting.RESET;
            final String canSubstitute = substitute ? GuiText.Yes.getLocal() : GuiText.No.getLocal();
            final String canBeSubstitute = beSubstitute ? GuiText.Yes.getLocal() : GuiText.No.getLocal();
            final String label = (isCrafting ? GuiText.Crafts.getLocal() : GuiText.Creates.getLocal());
            final String with = GuiText.With.getLocal();
            final String result = (EnumChatFormatting.DARK_AQUA + label) + ": " + EnumChatFormatting.RESET;
            final String ingredients = (EnumChatFormatting.DARK_GREEN + with) + ": " + EnumChatFormatting.RESET;
            final String holdShift = I18n.format(NameConst.TT_SHIFT_FOR_MORE) + EnumChatFormatting.RESET;

            recipeIsBroken = addInformation(inItems, in, ingredients, EnumChatFormatting.GREEN) || recipeIsBroken;
            recipeIsBroken = addInformation(outItems, out, result, EnumChatFormatting.AQUA) || recipeIsBroken;

            if (recipeIsBroken) {
                lines.add(EnumChatFormatting.RED + GuiText.InvalidPattern.getLocal());
            } else {
                lines.addAll(out);
                if (GuiScreen.isShiftKeyDown()) {
                    lines.addAll(in);
                } else {
                    lines.add(holdShift);
                }

                lines.add(substitutionLabel + canSubstitute);
                lines.add(beSubstitutionLabel + canBeSubstitute);

                if (!StringUtils.isNullOrEmpty(author)) {
                    lines.add(
                        EnumChatFormatting.LIGHT_PURPLE + GuiText.EncodedBy.getLocal(author)
                            + EnumChatFormatting.RESET);
                }
            }
            lines.add(lastLine);
        }
        return lines;
    }

    private boolean addInformation(final IAEStack<?>[] items, final List<String> lines, String label,
        EnumChatFormatting color) {
        final ItemStack unknownItem = new ItemStack(Blocks.fire);
        unknownItem.setStackDisplayName(GuiText.UnknownItem.getLocal());
        boolean recipeIsBroken = false;
        boolean first = true;
        List<IAEStack<?>> sortedItems = new ArrayList<>(items.length);
        for (IAEStack<?> item : items) {
            if (item != null) {
                sortedItems.add(item);
            }
        }
        sortedItems.sort(
            Comparator.<IAEStack<?>>comparingLong(IAEStack::getStackSize)
                .reversed());

        for (final IAEStack<?> item : sortedItems) {

            if (!recipeIsBroken && item instanceof IAEItemStack itemStack && itemStack.equals(unknownItem)) {
                recipeIsBroken = true;
            }

            final boolean isFluid = item.isFluid();
            final String displayName = item.isItem() ? Util.getDisplayName((IAEItemStack) item) : item.getDisplayName();
            final String unit = item.getStackType()
                .getDisplayUnit();

            if (first) {
                lines.add(label);
                lines.add(
                    "   " + EnumChatFormatting.WHITE
                        + NumberFormat.getNumberInstance(Locale.US)
                            .format(item.getStackSize())
                        + EnumChatFormatting.RESET
                        + (isFluid ? EnumChatFormatting.WHITE + unit + " " : " ")
                        + EnumChatFormatting.RESET
                        + color
                        + displayName);
            }
            if (!first) {
                lines.add(
                    "   " + EnumChatFormatting.WHITE
                        + NumberFormat.getNumberInstance(Locale.US)
                            .format(item.getStackSize())
                        + EnumChatFormatting.RESET
                        + (isFluid ? EnumChatFormatting.WHITE + unit + " " : " ")
                        + EnumChatFormatting.RESET
                        + color
                        + displayName);
            }

            first = false;
        }

        return recipeIsBroken;
    }

}

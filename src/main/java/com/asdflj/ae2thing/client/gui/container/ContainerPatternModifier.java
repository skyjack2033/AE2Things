package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.client.gui.container.slot.SlotEncodedPatternInput;
import com.asdflj.ae2thing.client.gui.container.slot.SlotReplaceFake;
import com.asdflj.ae2thing.inventory.item.PatternModifierInventory;
import com.asdflj.ae2thing.util.PatternModifierStacks;
import com.asdflj.ae2thing.util.PatternStackCodec;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

public class ContainerPatternModifier extends AEBaseContainer implements IPatternValueContainer {

    private final PatternModifierInventory it;
    private final SlotRestrictedInput[] pattern = new SlotRestrictedInput[36];
    private final SlotFake replaceSource;
    private final SlotFake replaceTarget;
    private static final ItemStack encodePattern = AEApi.instance()
        .definitions()
        .items()
        .encodedPattern()
        .maybeStack(1)
        .get();
    private final IInventory patterns;
    private final IInventory replace;

    public ContainerPatternModifier(InventoryPlayer ip, ITerminalHost host) {
        super(ip, host);
        this.it = (PatternModifierInventory) host;
        this.patterns = this.it.getInventoryByName(Constants.PATTERN);
        this.replace = this.it.getInventoryByName(Constants.REPLACE);
        for (int i = 0; i < this.patterns.getSizeInventory(); i++) {
            int x = (i % 9) * 18 + 8;
            int y = (i / 9) * 18 + 19;
            this.addSlotToContainer(this.pattern[i] = new SlotEncodedPatternInput(this.patterns, i, x, y, ip));
        }
        this.addSlotToContainer(this.replaceSource = new SlotReplaceFake(this.replace, 0, 8, 93));
        this.addSlotToContainer(this.replaceTarget = new SlotReplaceFake(this.replace, 1, 50, 93));
        this.lockPlayerInventorySlot(it.getInventorySlot());
        this.bindPlayerInventory(ip, 0, 125);
    }

    public ItemStack getSource() {
        return replaceSource.getStack();
    }

    public void clearPattern() {
        int blankPattern = 0;
        for (int i = 0; i < this.patterns.getSizeInventory(); i++) {
            ItemStack itemStack = this.patterns.getStackInSlot(i);
            if (itemStack != null) {
                blankPattern++;
                this.patterns.setInventorySlotContents(i, null);
            }
        }
        if (blankPattern <= 0) return;
        ItemStack pattern = AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .maybeStack(blankPattern)
            .get();
        if (!getPlayerInv().addItemStackToInventory(pattern)) {
            this.dropItem(pattern);
        }
    }

    public Slot getTargetSlot() {
        return this.replaceTarget;
    }

    public Slot getSourceSlot() {
        return this.replaceSource;
    }

    protected void dropItem(ItemStack is) {
        if (is == null || is.stackSize <= 0) return;
        ItemStack itemStack = is.copy();
        int i = itemStack.getMaxStackSize();
        while (itemStack.stackSize > 0) {
            if (i > itemStack.stackSize) {
                if (!getPlayerInv().addItemStackToInventory(itemStack.copy())) {
                    getPlayerInv().player.entityDropItem(itemStack.copy(), 0);
                }
                break;
            } else {
                itemStack.stackSize -= i;
                ItemStack item = itemStack.copy();
                item.stackSize = i;
                if (!getPlayerInv().addItemStackToInventory(item)) {
                    getPlayerInv().player.entityDropItem(item, 0);
                }
            }
        }
    }

    public void replacePattern() {
        if (!this.replaceSource.getHasStack()) return;
        ItemStack source = this.replaceSource.getStack();
        ItemStack target = this.replaceTarget.getStack();
        IAEStack<?> nativeSource = PatternStackCodec.toPatternStack(source);
        IAEStack<?> nativeTarget = PatternStackCodec.toPatternStack(target);
        if (nativeSource == null || target != null && nativeTarget == null) return;

        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem cpi)) continue;
            try {
                // Helpers may cache a failed parse in the supplied NBT, so inspect a copy.
                ICraftingPatternDetails details = cpi
                    .getPatternForItem(stack.copy(), this.getInventoryPlayer().player.worldObj);
                if (details == null) continue;
                IAEStack<?>[] in = this.replacePattern(details.getAEInputs(), nativeSource, nativeTarget, details);
                IAEStack<?>[] out = this.replacePattern(details.getAEOutputs(), nativeSource, nativeTarget, details);
                if (in == null || out == null) continue;
                if (details.isCraftable()) {
                    encode(details, in, out, i);
                } else {
                    encodeProcessingPattern(details, in, out, i);
                }
            } catch (RuntimeException ignored) {
                // Keep a malformed pattern intact and continue updating the other slots.
            }
        }
    }

    private void encodeProcessingPattern(ICraftingPatternDetails details, IAEStack<?>[] in, IAEStack<?>[] out,
        int slot) {
        NBTTagCompound previous = details.getPattern()
            .getTagCompound();
        NBTTagCompound data = PatternStackCodec.processingData(
            in,
            out,
            details.canSubstitute(),
            details.canBeSubstitute(),
            previous != null && previous.getBoolean("prioritize"));
        if (data == null) return;
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .orNull();
        if (pattern == null) return;
        pattern.setTagCompound(data);
        stampAuthor(pattern);
        if (((ICraftingPatternItem) pattern.getItem())
            .getPatternForItem(pattern, this.getInventoryPlayer().player.worldObj) != null) {
            patterns.setInventorySlotContents(slot, pattern);
        }
    }

    protected ItemStack stampAuthor(ItemStack patternStack) {
        if (patternStack.stackTagCompound == null) {
            patternStack.stackTagCompound = new NBTTagCompound();
        }
        patternStack.stackTagCompound.setString("author", this.getPlayerInv().player.getCommandSenderName());
        return patternStack;
    }

    private void encode(ICraftingPatternDetails cpi, IAEStack<?>[] in, IAEStack<?>[] out, int slot) {
        NBTTagList inList = list2tagList(in);
        NBTTagList outList = list2tagList(out);
        if (inList == null || outList == null) return;
        NBTTagCompound tag = (NBTTagCompound) Platform.openNbtData(cpi.getPattern())
            .copy();
        tag.removeTag("InvalidPattern");
        tag.setTag("in", inList);
        tag.setTag("out", outList);
        ItemStack cp = encodePattern.copy();
        cp.setTagCompound(tag);
        if (((ICraftingPatternItem) cp.getItem()).getPatternForItem(cp, this.getInventoryPlayer().player.worldObj)
            != null) {
            patterns.setInventorySlotContents(slot, cp);
        }
    }

    private NBTTagList list2tagList(IAEStack<?>[] list) {
        NBTTagList nbtTagList = new NBTTagList();
        for (IAEStack<?> is : list) {
            if (is == null) {
                nbtTagList.appendTag(new NBTTagCompound());
            } else {
                if (!(is instanceof IAEItemStack item)) return null;
                nbtTagList.appendTag(createItemTag(item.getItemStack()));
            }
        }
        return nbtTagList;
    }

    private IAEStack<?>[] replacePattern(IAEStack<?>[] list, IAEStack<?> source, IAEStack<?> target,
        ICraftingPatternDetails details) {
        return PatternModifierStacks.replace(
            list,
            source,
            target,
            slot -> !details.isCraftable() || target instanceof IAEItemStack item
                && details.isValidItemForSlot(slot, item.getItemStack(), this.getPlayerInv().player.worldObj));
    }

    protected NBTBase createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();
        if (i != null) {
            Util.writeItemStackToNBT(i, c);
        }
        return c;
    }

    @Override
    public boolean isValidContainer() {
        return true;
    }
}

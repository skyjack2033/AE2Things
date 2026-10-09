package com.asdflj.ae2thing.client.gui.container.widget;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.container.IPatternContainer;
import com.asdflj.ae2thing.client.gui.container.slot.SlotPattern;
import com.asdflj.ae2thing.client.gui.container.slot.SlotPatternFake;
import com.asdflj.ae2thing.inventory.IPatternTerminal;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.util.PatternStackCodec;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class PatternContainer implements IPatternContainer, IOptionalSlotHost, IWidgetSlot {

    protected final IInventory crafting;
    protected final IInventory craftingEx;
    protected final IInventory outputEx;
    protected final IInventory patternInv;
    protected final SlotPattern patternSlotIN;
    protected final SlotPattern patternSlotOUT;
    protected SlotPattern patternRefiller;
    protected SlotPatternFake[] craftingExSlots;
    protected SlotPatternFake[] outputExSlots;
    protected SlotFake[] craftingSlots;
    protected SlotPatternTerm craftSlot;
    private static final int CRAFTING_GRID_PAGES = 2;
    private static final int CRAFTING_GRID_WIDTH = 4;
    private static final int CRAFTING_GRID_HEIGHT = 4;
    private static final int CRAFTING_GRID_SLOTS = CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT;
    protected final AppEngInternalInventory cOut = new AppEngInternalInventory(null, 1);
    private final IPatternTerminal it;
    private final ContainerWirelessDualInterfaceTerminal container;
    private final List<Slot> slots = new ArrayList<>();
    private final ITerminalHost host;

    public PatternContainer(InventoryPlayer ip, ITerminalHost host, ContainerWirelessDualInterfaceTerminal container) {
        this.container = container;
        this.it = (IPatternTerminal) host;
        this.host = host;
        this.crafting = this.it.getInventoryByName(Constants.CRAFTING);
        this.craftingEx = this.it.getInventoryByName(Constants.CRAFTING_EX);
        this.outputEx = this.it.getInventoryByName(Constants.OUTPUT_EX);
        this.patternInv = this.it.getInventoryByName(Constants.PATTERN);
        this.craftingSlots = new SlotFakeCraftingMatrix[9];
        this.craftingExSlots = new SlotPatternFake[CRAFTING_GRID_SLOTS * CRAFTING_GRID_PAGES];
        this.outputExSlots = new SlotPatternFake[CRAFTING_GRID_SLOTS * CRAFTING_GRID_PAGES];
        this.addMESlotToContainer(
            this.patternSlotIN = new SlotPattern(
                SlotRestrictedInput.PlacableItemType.BLANK_PATTERN,
                patternInv,
                0,
                220,
                31,
                ip));
        this.slots.add(this.patternSlotIN);
        this.addMESlotToContainer(
            this.patternSlotOUT = new SlotPattern(
                SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
                patternInv,
                1,
                220,
                31 + 43,
                ip));
        this.patternSlotOUT.setStackLimit(1);
        this.slots.add(this.patternSlotOUT);
        if (this.isPatternTerminal()) {
            this.addMESlotToContainer(
                this.patternRefiller = new SlotPattern(
                    SlotRestrictedInput.PlacableItemType.UPGRADES,
                    this.it.getInventoryByName(Constants.UPGRADES),
                    0,
                    217,
                    110,
                    this.container.getInventoryPlayer()));
            this.slots.add(this.patternRefiller);
        }
        for (int page = 0; page < CRAFTING_GRID_PAGES; page++) {
            for (int y = 0; y < CRAFTING_GRID_HEIGHT; y++) {
                for (int x = 0; x < CRAFTING_GRID_WIDTH; x++) {
                    this.addMESlotToContainer(
                        this.craftingExSlots[x + y * CRAFTING_GRID_WIDTH
                            + page * CRAFTING_GRID_SLOTS] = new SlotPatternFake(
                                craftingEx,
                                this,
                                x + y * CRAFTING_GRID_WIDTH + page * CRAFTING_GRID_SLOTS,
                                224,
                                -59,
                                x,
                                y,
                                x + 4));
                    this.slots.add(this.craftingExSlots[x + y * CRAFTING_GRID_WIDTH + page * CRAFTING_GRID_SLOTS]);
                }
            }
            for (int x = 0; x < CRAFTING_GRID_WIDTH; x++) {
                for (int y = 0; y < CRAFTING_GRID_HEIGHT; y++) {
                    this.addMESlotToContainer(
                        this.outputExSlots[x * CRAFTING_GRID_HEIGHT + y
                            + page * CRAFTING_GRID_SLOTS] = new SlotPatternFake(
                                outputEx,
                                this,
                                x * CRAFTING_GRID_HEIGHT + y + page * CRAFTING_GRID_SLOTS,
                                224 + 97,
                                -59,
                                -x,
                                y,
                                x));
                    this.slots.add(this.outputExSlots[x * CRAFTING_GRID_HEIGHT + y + page * CRAFTING_GRID_SLOTS]);
                }
            }
        }
        this.addMESlotToContainer(
            this.craftSlot = new SlotPatternTerm(
                ip.player,
                this.container.getActionSource(),
                this.container.getPowerSource(),
                this.host,
                this.crafting,
                patternInv,
                this.cOut,
                224 + 92,
                -32,
                this,
                0,
                this.container));
        this.craftSlot.setIIcon(-1);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addMESlotToContainer(
                    this.craftingSlots[x
                        + y * 3] = new SlotFakeCraftingMatrix(this.crafting, x + y * 3, 224 + x * 18, -50 + y * 18));
            }
        }

        if (this.hasRefillerUpgrade()) {
            refillBlankPatterns(patternSlotIN);
        }
    }

    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            this.container.substitute = this.it.isSubstitution() && allowsInputSubstitution();
            this.container.combine = this.it.shouldCombine();
            this.container.beSubstitute = this.it.canBeSubstitute();
            this.container.craftingMode = this.it.isCraftingRecipe();
            if (container.inverted != it.isInverted() || container.activePage != it.getActivePage()) {
                container.inverted = it.isInverted();
                container.activePage = it.getActivePage();
                updateOrderOfOutputSlots();
            }
            if (this.container.isCraftingMode() != this.it.isCraftingRecipe()) {
                this.container.setCraftingMode(this.it.isCraftingRecipe());
                this.updateOrderOfOutputSlots();
            }
        }
    }

    public boolean allowsInputSubstitution() {
        boolean craftingMode = Platform.isServer() ? this.it.isCraftingRecipe() : this.container.isCraftingMode();
        return craftingMode || !containsFluidMarker(this.craftingEx) && !containsFluidMarker(this.outputEx);
    }

    private static boolean containsFluidMarker(IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (PatternStackCodec.isFluidMarker(inventory.getStackInSlot(slot))) return true;
        }
        return false;
    }

    private void updateOrderOfOutputSlots() {
        if (this.container.isCraftingMode()) {
            this.craftSlot.xDisplayPosition = this.craftSlot.getX();
            for (SlotFake slot : this.craftingSlots) {
                slot.xDisplayPosition = slot.getX();
            }
            for (SlotPatternFake slot : this.outputExSlots) {
                slot.setHidden(true);
            }
            for (SlotPatternFake slot : this.craftingExSlots) {
                slot.setHidden(true);
            }
        } else {
            this.craftSlot.xDisplayPosition = -9000;
            for (SlotFake slot : this.craftingSlots) {
                slot.xDisplayPosition = -9000;
            }
            for (SlotPatternFake slot : this.outputExSlots) {
                slot.setHidden(false);
            }
            for (SlotPatternFake slot : this.craftingExSlots) {
                slot.setHidden(false);
            }
            offsetSlots();
        }
    }

    private void offsetSlots() {
        for (int page = 0; page < CRAFTING_GRID_PAGES; page++) {
            for (int y = 0; y < CRAFTING_GRID_HEIGHT; y++) {
                for (int x = 0; x < CRAFTING_GRID_WIDTH; x++) {
                    this.craftingExSlots[x + y * CRAFTING_GRID_WIDTH + page * CRAFTING_GRID_SLOTS]
                        .setHidden(page != container.activePage || x > 0 && container.inverted);
                    this.outputExSlots[x * CRAFTING_GRID_HEIGHT + y + page * CRAFTING_GRID_SLOTS]
                        .setHidden(page != container.activePage || x > 0 && !container.inverted);
                }
            }
        }
    }

    public void onUpdate(String field, Object oldValue, Object newValue) {
        if (field.equals("inverted") || field.equals("activePage")) {
            updateOrderOfOutputSlots();
        }
        if (field.equals("craftingMode")) {
            this.getAndUpdateOutput();
            this.updateOrderOfOutputSlots();
        }
    }

    private final ItemStack[] recipeCache = new ItemStack[10];

    public ItemStack getAndUpdateOutput() {
        if (!this.container.isCraftingMode()) return null;
        boolean sameRecipe = true;
        for (int i = 0; i < this.crafting.getSizeInventory(); i++) {
            if (recipeCache[i] == null && this.crafting.getStackInSlot(i) == null) continue;
            if (!Platform.isSameItemPrecise(recipeCache[i], this.crafting.getStackInSlot(i))) {
                sameRecipe = false;
                break;
            }
        }

        if (!sameRecipe) {
            final InventoryCrafting ic = new InventoryCrafting(this.container, 3, 3);
            for (int x = 0; x < ic.getSizeInventory(); x++) {
                ic.setInventorySlotContents(x, this.crafting.getStackInSlot(x));
            }

            final ItemStack is = CraftingManager.getInstance()
                .findMatchingRecipe(ic, this.container.getPlayerInv().player.worldObj);
            this.cOut.setInventorySlotContents(0, is);
            for (int i = 0; i < this.crafting.getSizeInventory(); i++) {
                recipeCache[i] = this.crafting.getStackInSlot(i);
            }
            recipeCache[9] = is;
            return is;
        } else if (recipeCache[9] != null) {
            return recipeCache[9];
        }
        return null;
    }

    protected void addMESlotToContainer(AppEngSlot newSlot) {
        this.container.addMESlotToContainer(newSlot);
    }

    @Override
    public IPatternTerminal getPatternTerminal() {
        return this.it;
    }

    @Override
    public void clear() {
        for (final Slot s : this.craftingExSlots) {
            s.putStack(null);
        }
        for (final Slot s : this.outputExSlots) {
            s.putStack(null);
        }
        for (final Slot s : this.craftingSlots) {
            s.putStack(null);
        }
        this.getAndUpdateOutput();
        this.detectAndSendChanges();
    }

    @Override
    public void doubleStacks(int val) {
        if (this.container.isCraftingMode()) return;
        boolean isShift = (val & 1) != 0;
        boolean backwards = (val & 2) != 0;
        int multi = isShift ? 8 : 2;
        multi = backwards ? Math.negateExact(multi) : multi;
        if (canDouble(this.craftingExSlots, multi) && canDouble(this.outputExSlots, multi)) {
            doubleStacksInternal(this.craftingExSlots, multi);
            doubleStacksInternal(this.outputExSlots, multi);
        }
        this.detectAndSendChanges();
    }

    @Override
    public Slot getPatternOutputSlot() {
        return this.patternSlotOUT;
    }

    @Override
    public boolean isPatternTerminal() {
        return true;
    }

    @Override
    public boolean hasRefillerUpgrade() {
        return this.getPatternTerminal()
            .hasRefillerUpgrade();
    }

    @Override
    public void refillBlankPatterns(Slot slot) {
        if (Platform.isServer() && this.it instanceof WirelessTerminal wt) {
            ItemStack blanks = slot.getStack();
            int blanksToRefill = 64;
            if (blanks != null) blanksToRefill -= blanks.stackSize;
            if (blanksToRefill <= 0) return;
            final AEItemStack request = AEItemStack.create(
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(blanksToRefill)
                    .get());
            final IAEItemStack extracted = Platform
                .poweredExtraction(wt, wt.getItemInventory(), request, wt.getActionSource());
            if (extracted != null) {
                if (blanks != null) blanks.stackSize += extracted.getStackSize();
                else {
                    blanks = extracted.getItemStack();
                }
                slot.putStack(blanks);
            }
        }
    }

    protected ItemStack[] getInputs() {
        final ArrayList<ItemStack> input = new ArrayList<>();
        boolean hasInput = false;
        if (this.container.isCraftingMode()) {
            for (SlotFake craftingSlot : this.craftingSlots) {
                ItemStack stack = craftingSlot.getStack();
                input.add(stack);
                hasInput |= stack != null;
            }
            if (hasInput) {
                return input.toArray(new ItemStack[0]);
            }
        } else {
            for (SlotFake craftingSlot : this.craftingExSlots) {
                ItemStack stack = craftingSlot.getStack();
                input.add(stack);
                hasInput |= stack != null;
            }
            if (hasInput) {
                return input.toArray(new ItemStack[0]);
            }
        }

        return null;
    }

    protected ItemStack[] getOutputs() {
        final ArrayList<ItemStack> output = new ArrayList<>();
        if (this.container.isCraftingMode()) {
            final ItemStack out = this.getAndUpdateOutput();
            if (out != null && out.stackSize > 0) {
                return new ItemStack[] { out };
            }
        } else {
            boolean hasOutput = false;
            for (final SlotFake outputSlot : this.outputExSlots) {
                ItemStack stack = outputSlot.getStack();
                output.add(stack);
                hasOutput |= stack != null;
            }
            if (hasOutput) {
                return output.toArray(new ItemStack[0]);
            }
        }
        return null;
    }

    protected boolean notPattern(final ItemStack output) {
        if (output == null) {
            return true;
        }
        if (output.getItem() instanceof ICraftingPatternItem) {
            return false;
        }
        final IDefinitions definitions = AEApi.instance()
            .definitions();

        boolean isPattern = definitions.items()
            .encodedPattern()
            .isSameAs(output);
        isPattern |= definitions.materials()
            .blankPattern()
            .isSameAs(output);

        return !isPattern;
    }

    public void encodeItemPattern() {
        encodeItemPatternWithResult();
    }

    private boolean encodeItemPatternWithResult() {
        ItemStack output = this.patternSlotOUT.getStack();
        final ItemStack[] in = this.getInputs();
        final ItemStack[] out = this.getOutputs();

        // if there is no input, this would be silly.
        if (in == null || out == null) {
            return false;
        }
        // first check the output slots, should either be null, or a pattern
        if (output != null && this.notPattern(output)) {
            return false;
        }
        if (output == null) {
            ItemStack blank = this.patternSlotIN.getStack();
            if (this.notPattern(blank) || blank.stackSize <= 0) return false;
        }
        // The item determines which parser reads the NBT. Never reuse an older processing-pattern item here.
        output = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .orNull();
        if (output == null) return false;

        // encode the slot.
        final NBTTagCompound encodedValue = new NBTTagCompound();

        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();

        for (final ItemStack i : in) {
            tagIn.appendTag(this.createItemTag(i));
        }

        for (final ItemStack i : out) {
            tagOut.appendTag(this.createItemTag(i));
        }

        encodedValue.setTag("in", tagIn);
        encodedValue.setTag("out", tagOut);
        encodedValue.setBoolean("crafting", this.container.craftingMode);
        encodedValue.setBoolean("substitute", this.container.substitute);
        encodedValue.setBoolean("beSubstitute", this.container.beSubstitute);
        output.setTagCompound(encodedValue);
        stampAuthor(output);
        return storeEncodedPattern(this.patternSlotIN, this.patternSlotOUT, output);
    }

    protected ItemStack stampAuthor(ItemStack patternStack) {
        if (patternStack.stackTagCompound == null) {
            patternStack.stackTagCompound = new NBTTagCompound();
        }
        patternStack.stackTagCompound.setString("author", this.container.getPlayerInv().player.getCommandSenderName());
        return patternStack;
    }

    protected NBTBase createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();
        if (i != null) {
            Util.writeItemStackToNBT(i, c);
        }
        return c;
    }

    @Override
    public void encode() {
        encodeForUpload();
    }

    /** Returns success for this encoding attempt, independently of any output left from an earlier recipe. */
    public boolean encodeForUpload() {
        if (this.hasRefillerUpgrade()) refillBlankPatterns(this.patternSlotIN);
        if (this.container.isCraftingMode()) {
            return encodeItemPatternWithResult();
        }
        ItemStack stack = this.patternSlotOUT.getStack();
        if (stack == null) {
            stack = this.patternSlotIN.getStack();
            if (notPattern(stack) || stack.stackSize <= 0) return false;
        } else if (notPattern(stack)) {
            return false;
        }
        return storeEncodedPattern(this.patternSlotIN, this.patternSlotOUT, createProcessingPattern());
    }

    static boolean storeEncodedPattern(Slot blankSlot, Slot outputSlot, ItemStack encoded) {
        if (encoded == null || encoded.stackSize <= 0) return false;
        ItemStack previous = outputSlot.getStack();
        ItemStack blanks = blankSlot.getStack();
        if (previous == null && (blanks == null || blanks.stackSize <= 0)) return false;

        outputSlot.putStack(encoded.copy());
        if (!ItemStack.areItemStacksEqual(encoded, outputSlot.getStack())) {
            outputSlot.putStack(previous);
            return false;
        }
        if (previous == null) {
            ItemStack remaining = blanks.stackSize == 1 ? null : blanks.copy();
            if (remaining != null) remaining.stackSize--;
            blankSlot.putStack(remaining == null ? null : remaining.copy());
            if (!ItemStack.areItemStacksEqual(remaining, blankSlot.getStack())) {
                blankSlot.putStack(blanks);
                outputSlot.putStack(null);
                return false;
            }
        }
        return true;
    }

    protected static IAEStack<?>[] collectInventory(Slot[] slots) {
        IAEStack<?>[] stacks = new IAEStack<?>[slots.length];
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = slots[i].getStack();
            stacks[i] = PatternStackCodec.toPatternStack(stack);
            // A malformed fluid slot must fail the whole encode, not silently disappear from the recipe.
            if (stack != null && stacks[i] == null) return null;
        }
        return stacks;
    }

    private ItemStack createProcessingPattern() {
        NBTTagCompound data = PatternStackCodec.processingData(
            collectInventory(this.craftingExSlots),
            collectInventory(this.outputExSlots),
            this.container.substitute,
            this.container.beSubstitute);
        if (data == null) return null;
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .orNull();
        if (pattern == null) return null;
        pattern.setTagCompound(data);
        stampAuthor(pattern);
        ICraftingPatternItem item = (ICraftingPatternItem) pattern.getItem();
        if (item.getPatternForItem(pattern, this.container.getPlayerInv().player.worldObj) == null) return null;
        return pattern;
    }

    @Override
    public void encodeAndMoveToInventory() {
        this.encode();
        ItemStack output = this.patternSlotOUT.getStack();
        if (output != null) {
            if (!this.container.getPlayerInv()
                .addItemStackToInventory(output)) {
                this.container.getPlayerInv().player.entityDropItem(output, 0);
            }
            this.patternSlotOUT.putStack(null);
        }
        if (this.hasRefillerUpgrade()) refillBlankPatterns(patternSlotIN);
    }

    @Override
    public void encodeAllItemAndMoveToInventory() {
        this.encode();
        ItemStack output = this.patternSlotOUT.getStack();
        if (output != null) {
            if (this.patternSlotIN.getStack() != null) output.stackSize += this.patternSlotIN.getStack().stackSize;
            if (!this.container.getPlayerInv()
                .addItemStackToInventory(output)) {
                this.container.getPlayerInv().player.entityDropItem(output, 0);
            }
            this.patternSlotOUT.putStack(null);
            this.patternSlotIN.putStack(null);
        }
        if (this.hasRefillerUpgrade()) refillBlankPatterns(patternSlotIN);
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        if (idx < 4) // outputs
        {
            return this.container.inverted || idx == 0;
        } else {
            return !this.container.inverted || idx == 4;
        }
    }

    public void onSlotChange(Slot s) {
        if (s == this.patternSlotOUT && Platform.isServer()) {
            this.container.setInverted(this.it.isInverted());
            for (final Object crafter : this.container.getCrafters()) {
                final ICrafting icrafting = (ICrafting) crafter;

                for (final Object g : this.container.inventorySlots) {
                    if (g instanceof SlotFake sri) {
                        icrafting.sendSlotContents(this.container, sri.slotNumber, sri.getStack());
                    }
                }
                ((EntityPlayerMP) icrafting).isChangingQuantityOnly = false;
            }
            this.detectAndSendChanges();
        }

    }

    @Override
    public List<Slot> getSlot() {
        return this.slots;
    }
}

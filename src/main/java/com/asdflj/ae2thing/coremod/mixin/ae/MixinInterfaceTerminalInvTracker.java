package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.util.InterfacePatternInventory;
import com.asdflj.ae2thing.util.InterfaceTerminalNameCache;
import com.asdflj.ae2thing.util.InterfaceTerminalPatternCache;
import com.google.common.primitives.Ints;

import appeng.api.util.IInterfaceViewable;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

@Mixin(targets = "appeng.container.implementations.ContainerInterfaceTerminal$InvTracker", remap = false)
public abstract class MixinInterfaceTerminalInvTracker implements InterfacePatternInventory.Tracker {

    @Shadow
    @Final
    private long id;

    @Shadow
    @Final
    private IInventory patterns;

    @Shadow
    private NBTTagList invNbt;

    @Shadow
    private int numSlots;

    @Shadow
    private String name;

    @Shadow
    private String suffix;

    @Unique
    private IInterfaceViewable ae2thing$host;

    @Unique
    private boolean ae2thing$trackPatternChanges;

    @Unique
    private ItemStack[] ae2thing$lastPatterns;

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lappeng/api/util/IInterfaceViewable;getRawName()Ljava/lang/String;"),
        require = 1)
    private String ae2thing$cachedName(IInterfaceViewable host) {
        return InterfaceTerminalNameCache.getRawName(host);
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lappeng/api/util/IInterfaceViewable;rows()I"),
        require = 1)
    private int ae2thing$patternRows(IInterfaceViewable host) {
        return InterfacePatternInventory.getRows(host);
    }

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lappeng/api/util/IInterfaceViewable;numSlots()I"),
        require = 1)
    private int ae2thing$patternSlots(IInterfaceViewable host) {
        return InterfacePatternInventory.getSlotCount(host);
    }

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/util/IInterfaceViewable;getPatterns()Lnet/minecraft/inventory/IInventory;"),
        require = 1)
    private IInventory ae2thing$patternInventory(IInterfaceViewable host) {
        this.ae2thing$host = host;
        this.ae2thing$trackPatternChanges = InterfacePatternInventory.hasExtendedCapacity(host);
        return InterfacePatternInventory.getPatterns(host);
    }

    @Inject(method = "updateNBT()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void ae2thing$capturePatterns(CallbackInfo ci) {
        InterfaceTerminalPatternCache.Snapshot snapshot = InterfaceTerminalPatternCache
            .capture(this.ae2thing$host, this.patterns, this.numSlots);
        this.invNbt = snapshot.copyItemList();
        if (this.ae2thing$trackPatternChanges) {
            this.ae2thing$lastPatterns = snapshot.copyPatternReferences();
        }
        ci.cancel();
    }

    @Override
    public long ae2thing$getId() {
        return this.id;
    }

    @Override
    public PacketInterfaceTerminalUpdate ae2thing$syncName(PacketInterfaceTerminalUpdate update) {
        String rawName = InterfaceTerminalNameCache.getRawName(this.ae2thing$host);
        IChatComponent component = this.ae2thing$host.getNameSuffix();
        String currentSuffix = component == null ? null : IChatComponent.Serializer.func_150696_a(component);
        if (Objects.equals(this.name, rawName) && Objects.equals(this.suffix, currentSuffix)) return update;
        if (update == null) update = new PacketInterfaceTerminalUpdate();
        update.addRenamedEntry(this.id, rawName, currentSuffix, this.ae2thing$host.getDisplayRep());
        this.name = rawName;
        this.suffix = currentSuffix;
        return update;
    }

    @Override
    public NBTTagCompound ae2thing$updatePattern(int slot) {
        ItemStack current = this.patterns.getStackInSlot(slot);
        NBTTagCompound item = InterfaceTerminalPatternCache
            .updateSlot(this.ae2thing$host, this.patterns, this.numSlots, slot);
        this.invNbt.func_150304_a(slot, item);
        if (this.ae2thing$lastPatterns != null) {
            this.ae2thing$lastPatterns[slot] = ItemStack.copyItemStack(current);
        }
        return item;
    }

    @Override
    public PacketInterfaceTerminalUpdate ae2thing$syncPatternChanges(PacketInterfaceTerminalUpdate update) {
        if (this.ae2thing$lastPatterns == null) return update;
        List<Integer> changedSlots = null;
        NBTTagList items = null;
        for (int slot = 0; slot < this.ae2thing$lastPatterns.length; slot++) {
            ItemStack current = this.patterns.getStackInSlot(slot);
            if (ItemStack.areItemStacksEqual(current, this.ae2thing$lastPatterns[slot])) continue;
            if (changedSlots == null) {
                changedSlots = new ArrayList<>();
                items = new NBTTagList();
            }
            changedSlots.add(slot);
            items.appendTag(this.ae2thing$updatePattern(slot));
        }
        if (changedSlots != null) {
            if (update == null) update = new PacketInterfaceTerminalUpdate();
            update.addOverwriteEntry(this.id)
                .setItems(Ints.toArray(changedSlots), items);
        }
        return update;
    }
}

package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.util.InterfacePatternInventory;
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

    @Unique
    private boolean ae2thing$trackPatternChanges;

    @Unique
    private ItemStack[] ae2thing$lastPatterns;

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
        this.ae2thing$trackPatternChanges = InterfacePatternInventory.hasExtendedCapacity(host);
        return InterfacePatternInventory.getPatterns(host);
    }

    @Inject(method = "updateNBT()V", at = @At("TAIL"), require = 1)
    private void ae2thing$capturePatterns(CallbackInfo ci) {
        if (!this.ae2thing$trackPatternChanges) return;
        this.ae2thing$lastPatterns = new ItemStack[this.patterns.getSizeInventory()];
        for (int slot = 0; slot < this.ae2thing$lastPatterns.length; slot++) {
            this.ae2thing$lastPatterns[slot] = ItemStack.copyItemStack(this.patterns.getStackInSlot(slot));
        }
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
            NBTTagCompound item = new NBTTagCompound();
            if (current != null) current.writeToNBT(item);
            changedSlots.add(slot);
            items.appendTag(item);
            this.invNbt.func_150304_a(slot, item);
            this.ae2thing$lastPatterns[slot] = ItemStack.copyItemStack(current);
        }
        if (changedSlots != null) {
            if (update == null) update = new PacketInterfaceTerminalUpdate();
            update.addOverwriteEntry(this.id)
                .setItems(Ints.toArray(changedSlots), items);
        }
        return update;
    }
}

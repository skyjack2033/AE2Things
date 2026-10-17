package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.network.SPacketInterfaceRecipeMaps;
import com.asdflj.ae2thing.util.InterfacePatternInventory;
import com.asdflj.ae2thing.util.InterfaceRecipeMaps;

import appeng.api.util.IInterfaceViewable;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

@Mixin(value = ContainerInterfaceTerminal.class, remap = false)
public abstract class MixinContainerInterfaceTerminal {

    @Shadow
    @Final
    private Map<IInterfaceViewable, ?> tracked;

    @Unique
    private int ae2thing$recipeMapRefreshTicks;

    @Unique
    private boolean ae2thing$recipeMapRefreshPending;

    @Unique
    private Map<Long, Set<String>> ae2thing$lastRecipeMaps;

    @Inject(method = "detectAndSendChanges", at = @At("HEAD"), remap = true, require = 1)
    private void ae2thing$scheduleRecipeMaps(CallbackInfo ci) {
        if (this.ae2thing$getWirelessContainer() == null) return;
        if (this.ae2thing$lastRecipeMaps == null || ++this.ae2thing$recipeMapRefreshTicks >= 10) {
            this.ae2thing$recipeMapRefreshTicks = 0;
            this.ae2thing$recipeMapRefreshPending = true;
            ((ContainerInterfaceTerminal) (Object) this).scheduleUpdate();
        }
    }

    @Inject(method = "updateList", at = @At("RETURN"), require = 1)
    private void ae2thing$syncRecipeMaps(CallbackInfoReturnable<PacketInterfaceTerminalUpdate> cir) {
        ContainerWirelessDualInterfaceTerminal container = this.ae2thing$getWirelessContainer();
        if (container == null || !this.ae2thing$recipeMapRefreshPending) return;
        this.ae2thing$recipeMapRefreshPending = false;
        Map<Long, Set<String>> snapshot = InterfaceRecipeMaps.snapshot(this.tracked);
        if (snapshot.equals(this.ae2thing$lastRecipeMaps)) return;
        this.ae2thing$lastRecipeMaps = snapshot;
        AE2Thing.proxy.netHandler.sendTo(
            new SPacketInterfaceRecipeMaps(container.windowId, snapshot),
            (EntityPlayerMP) container.getPlayerInv().player);
    }

    @Unique
    private ContainerWirelessDualInterfaceTerminal ae2thing$getWirelessContainer() {
        EntityPlayer player = ((ContainerInterfaceTerminal) (Object) this).getPlayerInv().player;
        if (player instanceof EntityPlayerMP
            && player.openContainer instanceof ContainerWirelessDualInterfaceTerminal container
            && container.delegateContainer == (Object) this) return container;
        return null;
    }

    @Redirect(
        method = "updateList",
        at = @At(value = "INVOKE", target = "Lappeng/api/util/IInterfaceViewable;rows()I"),
        require = 1)
    private int ae2thing$patternRows(IInterfaceViewable host) {
        return InterfacePatternInventory.getRows(host);
    }

    @Redirect(
        method = "updateList",
        at = @At(value = "INVOKE", target = "Lappeng/api/util/IInterfaceViewable;numSlots()I"),
        require = 1)
    private int ae2thing$patternSlots(IInterfaceViewable host) {
        return InterfacePatternInventory.getSlotCount(host);
    }

    @Group(name = "ae2thing$syncPatternSlot", min = 1, max = 1)
    @Redirect(
        method = "syncIfaceSlot",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/container/implementations/ContainerInterfaceTerminal$InvTracker;updateNBT()V"),
        require = 0)
    private void ae2thing$syncPatternSlot(@Coerce Object tracker, @Coerce Object inv, long id, int slot,
        ItemStack stack) {
        ((InterfacePatternInventory.Tracker) tracker).ae2thing$updatePattern(slot);
    }

    @Group(name = "ae2thing$syncPatternSlot", min = 1, max = 1)
    @Redirect(
        method = "syncIfaceSlot",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/container/implementations/ContainerInterfaceTerminal$InvTracker;access$300(Lappeng/container/implementations/ContainerInterfaceTerminal$InvTracker;)V"),
        require = 0)
    private void ae2thing$syncPatternSlotAccessor(@Coerce Object tracker, @Coerce Object inv, long id, int slot,
        ItemStack stack) {
        ((InterfacePatternInventory.Tracker) tracker).ae2thing$updatePattern(slot);
    }

    @Inject(method = "updateList", at = @At("RETURN"), cancellable = true, require = 1)
    private void ae2thing$refreshPatterns(CallbackInfoReturnable<PacketInterfaceTerminalUpdate> cir) {
        // A fixed capacity no longer triggers AE2's inventory refresh when GTNL's dynamic rows change.
        PacketInterfaceTerminalUpdate update = cir.getReturnValue();
        for (Object tracker : this.tracked.values()) {
            if (tracker instanceof InterfacePatternInventory.Tracker extended) {
                update = extended.ae2thing$syncPatternChanges(update);
            }
        }
        cir.setReturnValue(update);
    }
}

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
import com.asdflj.ae2thing.util.InterfaceTerminalNameCache;
import com.asdflj.ae2thing.util.InterfaceTerminalRefresh;

import appeng.api.networking.IGridNode;
import appeng.api.util.IInterfaceViewable;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

@Mixin(value = ContainerInterfaceTerminal.class, remap = false)
public abstract class MixinContainerInterfaceTerminal {

    @Shadow
    @Final
    private Map<IInterfaceViewable, ?> tracked;

    @Unique
    private int ae2thing$listRefreshTicks;

    @Unique
    private InterfaceTerminalRefresh ae2thing$refresh;

    @Inject(method = "detectAndSendChanges", at = @At("HEAD"), remap = true, require = 1)
    private void ae2thing$scheduleList(CallbackInfo ci) {
        if (this.ae2thing$getWirelessContainer() == null) return;
        if (++this.ae2thing$listRefreshTicks >= 100) {
            this.ae2thing$listRefreshTicks = 0;
            ((ContainerInterfaceTerminal) (Object) this).scheduleUpdate();
        }
    }

    @Inject(method = "detectAndSendChanges", at = @At("TAIL"), remap = true, require = 1)
    private void ae2thing$refreshTracked(CallbackInfo ci) {
        ContainerWirelessDualInterfaceTerminal container = this.ae2thing$getWirelessContainer();
        if (container == null || !container.isValidContainer()) return;
        IGridNode node = container.getNetworkNode();
        if (node == null || !node.isActive()) return;
        if (this.ae2thing$refresh == null) this.ae2thing$refresh = new InterfaceTerminalRefresh();
        PacketInterfaceTerminalUpdate update = this.ae2thing$refresh.tick(this.tracked);
        EntityPlayerMP player = (EntityPlayerMP) container.getPlayerInv().player;
        if (update != null) {
            update.encode();
            NetworkHandler.instance.sendTo(update, player);
        }
        Map<Long, Set<String>> snapshot = this.ae2thing$refresh.takeRecipeMaps();
        if (snapshot != null) {
            AE2Thing.proxy.netHandler.sendTo(new SPacketInterfaceRecipeMaps(container.windowId, snapshot), player);
        }
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
        at = @At(value = "INVOKE", target = "Lappeng/api/util/IInterfaceViewable;getRawName()Ljava/lang/String;"),
        require = 1)
    private String ae2thing$cachedName(IInterfaceViewable host) {
        return InterfaceTerminalNameCache.getRawName(host);
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
        this.ae2thing$listRefreshTicks = 0;
        if (this.ae2thing$refresh != null) this.ae2thing$refresh.invalidate();
        if (this.ae2thing$getWirelessContainer() != null) return;
        PacketInterfaceTerminalUpdate update = cir.getReturnValue();
        for (Object tracker : this.tracked.values()) {
            if (tracker instanceof InterfacePatternInventory.Tracker extended) {
                update = extended.ae2thing$syncPatternChanges(update);
            }
        }
        cir.setReturnValue(update);
    }
}

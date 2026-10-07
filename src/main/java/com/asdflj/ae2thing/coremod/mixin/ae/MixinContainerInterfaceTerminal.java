package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.ae2thing.util.InterfacePatternInventory;

import appeng.api.util.IInterfaceViewable;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;

@Mixin(value = ContainerInterfaceTerminal.class, remap = false)
public abstract class MixinContainerInterfaceTerminal {

    @Shadow
    @Final
    private Map<IInterfaceViewable, ?> tracked;

    @Unique
    private int ae2thing$patternRefreshTicks;

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

    @Inject(method = "updateList", at = @At("RETURN"), cancellable = true, require = 1)
    private void ae2thing$refreshPatterns(CallbackInfoReturnable<PacketInterfaceTerminalUpdate> cir) {
        if (++this.ae2thing$patternRefreshTicks < 10) return;
        this.ae2thing$patternRefreshTicks = 0;
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

package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.ae2thing.network.CPacketTerminalBtns;
import com.asdflj.ae2thing.util.NameConst;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.container.AEBaseContainer;

@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends AEBaseGui {

    @Shadow(remap = false)
    private GuiAeButton start;

    @Shadow(remap = false)
    private GuiButton cancel;

    @Shadow(remap = false)
    @Final
    @Mutable
    private IItemList<IAEStack<?>> storage;
    @Shadow(remap = false)
    @Final
    @Mutable
    private IItemList<IAEStack<?>> pending;
    @Shadow(remap = false)
    @Final
    @Mutable
    private IItemList<IAEStack<?>> missing;
    @Shadow(remap = false)
    @Final
    private List<IAEStack<?>> visual;
    private GuiAeButton replan = null;
    private boolean clickStart = false;

    public MixinGuiCraftConfirm(Container container) {
        super(container);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = false)
    private void handleCustomCancel(GuiButton btn, CallbackInfo ci) {
        if (btn != this.cancel || !(this.inventorySlots instanceof AEBaseContainer container)) {
            return;
        }

        final Object target = container.getTarget();
        if (target instanceof PartInfusionPatternTerminal) {
            InventoryHandler.switchGui(GuiType.INFUSION_PATTERN_TERMINAL);
            ci.cancel();
        } else if (target instanceof WirelessDualInterfaceTerminalInventory) {
            InventoryHandler.switchGui(GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL);
            ci.cancel();
        }
    }

    @Inject(method = "actionPerformed", at = @At(value = "HEAD"))
    private void actionPerformed(GuiButton btn, CallbackInfo ci) {
        if (btn == start) {
            clickStart = true;
        } else if (btn == replan) {
            clickStart = false;
            start.enabled = false;
            replan.visible = false;
            /*
             * Rebuild instead of resetStatus(): zeroed entries stay in the list, and handleInput() only copies
             * stackSize
             * onto existing entries, dropping the incoming usedPercent and showing "<0.01%" after replanning.
             */
            this.storage = AEApi.instance()
                .storage()
                .createAEStackList();
            this.pending = AEApi.instance()
                .storage()
                .createAEStackList();
            this.missing = AEApi.instance()
                .storage()
                .createAEStackList();
            this.visual.clear();
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("GuiCraftConfirm.replan", true));
        }
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(
            replan = new GuiAeButton(
                0,
                start.xPosition,
                start.yPosition,
                start.width,
                start.height,
                I18n.format(NameConst.GUI_BUTTON_REPLAN),
                ""));
        this.replan.visible = false;
    }

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false)
    public void drawFG(CallbackInfo ci) {
        try {
            if (clickStart || !start.enabled) {
                replan.visible = true;
                start.visible = false;
            } else {
                replan.visible = false;
                start.visible = true;
            }
        } catch (Exception ignored) {}

    }
}

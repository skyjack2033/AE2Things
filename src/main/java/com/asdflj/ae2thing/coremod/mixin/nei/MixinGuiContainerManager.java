package com.asdflj.ae2thing.coremod.mixin.nei;

import static codechicken.nei.guihook.GuiContainerManager.getStackMouseOver;
import static com.asdflj.ae2thing.client.render.RenderHelper.canDrawPlus;
import static com.asdflj.ae2thing.client.render.RenderHelper.drawPlus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.client.gui.widget.IGuiMonitor;
import com.asdflj.ae2thing.nei.ButtonConstants;
import com.asdflj.ae2thing.nei.NEI_TH_Config;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.Util;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.me.ItemRepo;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.StackInfo;

@Mixin(GuiContainerManager.class)
public abstract class MixinGuiContainerManager {

    @Shadow(remap = false)
    public GuiContainer window;

    @Inject(
        method = "renderToolTips",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/guihook/GuiContainerManager;applyItemCountDetails(Ljava/util/List;Lnet/minecraft/item/ItemStack;)V"),
        remap = false)
    private void ae2thing$renderToolTips(int mousex, int mousey, CallbackInfo ci) {
        if (!NEI_TH_Config.getConfigValue(ButtonConstants.INVENTORY_STATE)) return;
        ItemStack stack;
        stack = getStackMouseOver(this.window);
        if (stack == null) return;
        if (window instanceof GuiRecipe<?>gui) {
            IDisplayRepo repo = null;
            if (gui.getFirstScreenGeneral() instanceof IGuiMonitor g) {
                repo = g.getRepo();
            } else if (AE2ThingAPI.instance()
                .terminal()
                .isTerminal(gui.getFirstScreenGeneral())) {
                    repo = Util.getDisplayRepo((AEBaseGui) gui.getFirstScreenGeneral());
                }
            if (!(repo instanceof ItemRepo)) return;
            IItemList<IAEStack<?>> list = Ae2ReflectClient.getList((ItemRepo) repo);
            FluidStack fs = StackInfo.getFluid(stack);
            IAEStack<?> target = fs == null ? AEItemStack.create(stack) : AEFluidStack.create(fs);
            IAEStack<?> found = target == null ? null : list.findPrecise(target);
            if (found != null) {
                ae2thing$render(found, mousex - 8, mousey - 40 < 0 ? mousey + 40 : mousey - 40);
            }
        }

    }

    private void ae2thing$render(IAEStack<?> stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0.0f, 0.0f, 350);
        stack.drawInGui(mc, x, y);
        stack.drawOverlayInGui(mc, x, y, true, false, true, false);
        GL11.glTranslatef(0.0f, 0.0f, -150.0f);
        if (stack.isCraftable() && canDrawPlus) {
            GL11.glTranslatef(0.0f, 0.0f, 450.0f);
            drawPlus(x, y);
            GL11.glTranslatef(0.0f, 0.0f, -450.0f);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}

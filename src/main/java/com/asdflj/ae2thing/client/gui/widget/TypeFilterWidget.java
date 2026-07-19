package com.asdflj.ae2thing.client.gui.widget;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.network.CPacketTypeFilter;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.widgets.TypeToggleButton;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStackType;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

/**
 * Shared client-side helper that renders {@link TypeToggleButton}s for the registered item and fluid stack types and
 * keeps a local copy of the per-type visibility map. GUIs delegate their {@code getTypeFilter()} to
 * {@link #getFilters()} so the shared {@code ItemRepo} can apply the filter, and route toggle clicks through
 * {@link #handleButtonClick(GuiButton)}.
 */
public class TypeFilterWidget {

    private static final int TYPE_BUTTON_VERTICAL_SPACING = 20;

    private final Map<TypeToggleButton, IAEStackType<?>> buttons = new IdentityHashMap<>();
    private Reference2BooleanMap<IAEStackType<?>> filters;
    private final int windowId;

    public TypeFilterWidget(int windowId) {
        this.windowId = windowId;
    }

    public void init(List<GuiButton> buttonList, int x, int yStart) {
        this.buttons.clear();
        if (this.filters == null) {
            return;
        }
        int y = yStart;
        for (final IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
            if ((type != AEItemStackType.ITEM_STACK_TYPE) && (type != AEFluidStackType.FLUID_STACK_TYPE)) {
                continue;
            }
            final ResourceLocation texture = type.getButtonTexture();
            final IIcon icon = type.getButtonIcon();
            if ((texture == null) || (icon == null)) {
                continue;
            }
            final TypeToggleButton button = new TypeToggleButton(x, y, texture, icon, type.getDisplayName());
            button.setEnabled(this.filters.getBoolean(type));
            this.buttons.put(button, type);
            buttonList.add(button);
            y += TYPE_BUTTON_VERTICAL_SPACING;
        }
    }

    public void setFilters(Reference2BooleanMap<IAEStackType<?>> filters) {
        this.filters = filters;
    }

    public Reference2BooleanMap<IAEStackType<?>> getFilters() {
        return this.filters;
    }

    /**
     * @return true when the click hit a type-toggle button and was handled.
     */
    public boolean handleButtonClick(GuiButton button) {
        if (!(button instanceof TypeToggleButton typeButton)) {
            return false;
        }
        final IAEStackType<?> type = this.buttons.get(typeButton);
        if ((type == null) || (this.filters == null)) {
            return false;
        }
        final boolean next = !this.filters.getBoolean(type);
        this.filters.put(type, next);
        typeButton.setEnabled(next);
        AE2Thing.proxy.netHandler.sendToServer(new CPacketTypeFilter(this.filters, this.windowId));
        return true;
    }
}

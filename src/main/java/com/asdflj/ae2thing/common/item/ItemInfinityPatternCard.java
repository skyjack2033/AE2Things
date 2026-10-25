package com.asdflj.ae2thing.common.item;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.util.NameConst;

import cpw.mods.fml.common.registry.GameRegistry;

public class ItemInfinityPatternCard extends BaseItem {

    public ItemInfinityPatternCard() {
        super();
        setMaxStackSize(1);
        setUnlocalizedName(NameConst.ITEM_INFINITY_PATTERN_CARD);
        setTextureName(
            AE2Thing.resource(NameConst.ITEM_PATTERN_MODIFIER)
                .toString());
    }

    @Override
    public ItemInfinityPatternCard register() {
        GameRegistry.registerItem(this, NameConst.ITEM_INFINITY_PATTERN_CARD, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }
}

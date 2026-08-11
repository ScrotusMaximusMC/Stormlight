package com.scrotey.stormlight.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WordsOfRadianceItem extends Item {
    public WordsOfRadianceItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}

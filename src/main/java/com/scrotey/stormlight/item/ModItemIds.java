package com.scrotey.stormlight.item;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {
    public static final ResourceKey<Item> DIAMOND_CHIP = create("diamond_chip");
    public static final ResourceKey<Item> DIAMOND_MARK = create("diamond_mark");
    public static final ResourceKey<Item> DIAMOND_BROAM = create("diamond_broam");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Stormlight.MOD_ID, name)
        );
    }
}
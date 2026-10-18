package com.scrotey.stormlight.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> CHIPS =
            create("chips");

    public static final TagKey<Item> MARKS =
            create("marks");

    public static final TagKey<Item> BROAMS =
            create("broams");

    private static TagKey<Item> create(String path) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(
                        "stormlight",
                        path
                )
        );
    }

    private ModItemTags() {
    }
}
package com.scrotey.stormlight.item;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {

    public static final ResourceKey<Item> SPHERE_POUCH = create("sphere_pouch");

    public static final ResourceKey<Item> DIAMOND_CHIP = create("diamond_chip");
    public static final ResourceKey<Item> DIAMOND_MARK = create("diamond_mark");
    public static final ResourceKey<Item> DIAMOND_BROAM = create("diamond_broam");

    public static final ResourceKey<Item> EMERALD_CHIP = create("emerald_chip");
    public static final ResourceKey<Item> EMERALD_MARK = create("emerald_mark");
    public static final ResourceKey<Item> EMERALD_BROAM = create("emerald_broam");


    public static final ResourceKey<Item> GARNET = create("garnet");
    public static final ResourceKey<Item> GARNET_CHIP = create("garnet_chip");
    public static final ResourceKey<Item> GARNET_MARK = create("garnet_mark");
    public static final ResourceKey<Item> GARNET_BROAM = create("garnet_broam");

    public static final ResourceKey<Item> RUBY = create("ruby");
    public static final ResourceKey<Item> RUBY_CHIP = create("ruby_chip");
    public static final ResourceKey<Item> RUBY_MARK = create("ruby_mark");
    public static final ResourceKey<Item> RUBY_BROAM = create("ruby_broam");

    public static final ResourceKey<Item> SAPPHIRE = create("sapphire");
    public static final ResourceKey<Item> SAPPHIRE_CHIP = create("sapphire_chip");
    public static final ResourceKey<Item> SAPPHIRE_MARK = create("sapphire_mark");
    public static final ResourceKey<Item> SAPPHIRE_BROAM = create("sapphire_broam");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Stormlight.MOD_ID, name)
        );
    }
}

package com.scrotey.stormlight.item;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {

    public static final ResourceKey<Item> RAW_DIAMOND_CHIP = create("raw_diamond_chip");
    public static final ResourceKey<Item> RAW_DIAMOND_MARK = create("raw_diamond_mark");
    public static final ResourceKey<Item> RAW_GARNET_CHIP = create("raw_garnet_chip");
    public static final ResourceKey<Item> RAW_GARNET_MARK = create("raw_garnet_mark");
    public static final ResourceKey<Item> RAW_RUBY_CHIP = create("raw_ruby_chip");
    public static final ResourceKey<Item> RAW_RUBY_MARK = create("raw_ruby_mark");
    public static final ResourceKey<Item> RAW_SAPPHIRE_CHIP = create("raw_sapphire_chip");
    public static final ResourceKey<Item> RAW_SAPPHIRE_MARK = create("raw_sapphire_mark");
    public static final ResourceKey<Item> RAW_EMERALD_CHIP = create("raw_emerald_chip");
    public static final ResourceKey<Item> RAW_EMERALD_MARK = create("raw_emerald_mark");

    public static final ResourceKey<Item> CHISEL = create("chisel");
    public static final ResourceKey<Item> CHIP_MOULD = create("chip_mould");
    public static final ResourceKey<Item> MARK_MOULD = create("mark_mould");
    public static final ResourceKey<Item> BROAM_MOULD = create("broam_mould");

    public static final ResourceKey<Item> SPHERE_POUCH = create("sphere_pouch");
    public static final ResourceKey<Item> MYSTERIOUS_BOOK =
            create("mysterious_book");
    public static final ResourceKey<Item> WAY_OF_KINGS = create("way_of_kings");
    public static final ResourceKey<Item> WORDS_OF_RADIANCE =
            create("words_of_radiance");

    public static final ResourceKey<Item> LARGE_DIAMOND = create("large_diamond");
    public static final ResourceKey<Item> LARGE_GARNET = create("large_garnet");
    public static final ResourceKey<Item> LARGE_RUBY = create("large_ruby");
    public static final ResourceKey<Item> LARGE_SAPPHIRE = create("large_sapphire");
    public static final ResourceKey<Item> LARGE_EMERALD = create("large_emerald");

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

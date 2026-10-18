package com.scrotey.stormlight.item;
import com.scrotey.stormlight.component.ModComponents;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Tool;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.function.Function;

public class ModItems {

    public static final Item RAW_DIAMOND_CHIP = register(
            ModItemIds.RAW_DIAMOND_CHIP, Item::new, new Item.Properties());
    public static final Item RAW_DIAMOND_BROAM = register(
            ModItemIds.RAW_DIAMOND_BROAM, Item::new, new Item.Properties());
    public static final Item RAW_DIAMOND_MARK = register(
            ModItemIds.RAW_DIAMOND_MARK, Item::new, new Item.Properties());
    public static final Item RAW_GARNET_CHIP = register(
            ModItemIds.RAW_GARNET_CHIP, Item::new, new Item.Properties());
    public static final Item RAW_GARNET_BROAM = register(
            ModItemIds.RAW_GARNET_BROAM, Item::new, new Item.Properties());
    public static final Item RAW_GARNET_MARK = register(
            ModItemIds.RAW_GARNET_MARK, Item::new, new Item.Properties());
    public static final Item RAW_RUBY_CHIP = register(
            ModItemIds.RAW_RUBY_CHIP, Item::new, new Item.Properties());
    public static final Item RAW_RUBY_BROAM = register(
            ModItemIds.RAW_RUBY_BROAM, Item::new, new Item.Properties());
    public static final Item RAW_RUBY_MARK = register(
            ModItemIds.RAW_RUBY_MARK, Item::new, new Item.Properties());
    public static final Item RAW_SAPPHIRE_CHIP = register(
            ModItemIds.RAW_SAPPHIRE_CHIP, Item::new, new Item.Properties());
    public static final Item RAW_SAPPHIRE_BROAM = register(
            ModItemIds.RAW_SAPPHIRE_BROAM, Item::new, new Item.Properties());
    public static final Item RAW_SAPPHIRE_MARK = register(
            ModItemIds.RAW_SAPPHIRE_MARK, Item::new, new Item.Properties());
    public static final Item RAW_EMERALD_CHIP = register(
            ModItemIds.RAW_EMERALD_CHIP, Item::new, new Item.Properties());
    public static final Item RAW_EMERALD_BROAM = register(
            ModItemIds.RAW_EMERALD_BROAM, Item::new, new Item.Properties());
    public static final Item RAW_EMERALD_MARK = register(
            ModItemIds.RAW_EMERALD_MARK, Item::new, new Item.Properties());

    public static final Item DIAMOND_CHISEL = register(
            ModItemIds.DIAMOND_CHISEL, Item::new, new Item.Properties().durability(1561));

    public static final Item CHISEL = register(
            ModItemIds.CHISEL, Item::new, new Item.Properties().durability(128));

    // A virtual, bound weapon: never added to ordinary inventory or creative tabs.
    public static final Item SHARDBLADE = register(
            ModItemIds.SHARDBLADE,
            ShardbladeItem::new,
            new Item.Properties()
                    .sword(ToolMaterial.NETHERITE, 3.0F, -2.4F)
                    // Sword defaults can refuse to start breaking blocks. The
                    // Shardblade is also a mining tool, including in creative.
                    .component(DataComponents.TOOL, new Tool(List.of(), 25.0F, 0, true))
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

    // Sphere forge moulds, one for each sphere size. They are reused for every pour.
    public static final Item CHIP_MOULD = register(
            ModItemIds.CHIP_MOULD, Item::new, new Item.Properties().stacksTo(16));
    public static final Item MARK_MOULD = register(
            ModItemIds.MARK_MOULD, Item::new, new Item.Properties().stacksTo(16));
    public static final Item BROAM_MOULD = register(
            ModItemIds.BROAM_MOULD, Item::new, new Item.Properties().stacksTo(16));

    public static final Item SPHERE_POUCH = register(
            ModItemIds.SPHERE_POUCH,
            SpherePouchItem::new,
            new Item.Properties()
                    .stacksTo(1)
    );

    public static final Item MYSTERIOUS_BOOK = register(
            ModItemIds.MYSTERIOUS_BOOK,
            Item::new,
            new Item.Properties().stacksTo(1)
    );

    public static final Item WAY_OF_KINGS = register(
            ModItemIds.WAY_OF_KINGS, Item::new, new Item.Properties().stacksTo(1)
    );

    public static final Item WORDS_OF_RADIANCE = register(
            ModItemIds.WORDS_OF_RADIANCE,
            WordsOfRadianceItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static final Item LARGE_DIAMOND = register(
            ModItemIds.LARGE_DIAMOND,
            Item::new,
            new Item.Properties()
    );

    public static final Item LARGE_GARNET = register(
            ModItemIds.LARGE_GARNET,
            Item::new,
            new Item.Properties()
    );

    public static final Item LARGE_RUBY = register(
            ModItemIds.LARGE_RUBY,
            Item::new,
            new Item.Properties()
    );

    public static final Item LARGE_SAPPHIRE = register(
            ModItemIds.LARGE_SAPPHIRE,
            Item::new,
            new Item.Properties()
    );

    public static final Item LARGE_EMERALD = register(
            ModItemIds.LARGE_EMERALD,
            Item::new,
            new Item.Properties()
    );

    public static final Item DIAMOND_CHIP = register(
            ModItemIds.DIAMOND_CHIP,
            properties -> new SphereItem(properties, 10, "diamond_chip"),
            sphereProperties()
    );

    public static final Item DIAMOND_MARK = register(
            ModItemIds.DIAMOND_MARK,
            properties -> new SphereItem(properties, 50, "diamond_mark"),
            sphereProperties()
    );

    public static final Item DIAMOND_BROAM = register(
            ModItemIds.DIAMOND_BROAM,
            properties -> new SphereItem(properties, 200, "diamond_broam"),
            sphereProperties()
    );

    public static final Item EMERALD_CHIP = register(
            ModItemIds.EMERALD_CHIP,
            properties -> new SphereItem(
                    properties,
                    50,
                    "emerald_chip"
            ),
            sphereProperties()
    );

    public static final Item EMERALD_MARK = register(
            ModItemIds.EMERALD_MARK,
            properties -> new SphereItem(
                    properties,
                    250,
                    "emerald_mark"
            ),
            sphereProperties()
    );

    public static final Item EMERALD_BROAM = register(
            ModItemIds.EMERALD_BROAM,
            properties -> new SphereItem(
                    properties,
                    1000,
                    "emerald_broam"
            ),
            sphereProperties()
    );

    public static final Item GARNET = register(
            ModItemIds.GARNET,
            Item::new,
            new Item.Properties()
    );

    public static final Item GARNET_CHIP = register(
            ModItemIds.GARNET_CHIP,
            properties -> new SphereItem(properties, 20, "garnet_chip"),
            sphereProperties()
    );

    public static final Item GARNET_MARK = register(
            ModItemIds.GARNET_MARK,
            properties -> new SphereItem(properties, 100, "garnet_mark"),
            sphereProperties()
    );

    public static final Item GARNET_BROAM = register(
            ModItemIds.GARNET_BROAM,
            properties -> new SphereItem(properties, 400, "garnet_broam"),
            sphereProperties()
    );

    public static final Item RUBY = register(
            ModItemIds.RUBY,
            Item::new,
            new Item.Properties()
    );

    public static final Item RUBY_CHIP = register(
            ModItemIds.RUBY_CHIP,
            properties -> new SphereItem(
                    properties,
                    30,
                    "ruby_chip"
            ),
            sphereProperties()
    );

    public static final Item RUBY_MARK = register(
            ModItemIds.RUBY_MARK,
            properties -> new SphereItem(
                    properties,
                    150,
                    "ruby_mark"
            ),
            sphereProperties()
    );

    public static final Item RUBY_BROAM = register(
            ModItemIds.RUBY_BROAM,
            properties -> new SphereItem(
                    properties,
                    600,
                    "ruby_broam"
            ),
            sphereProperties()
    );

    public static final Item SAPPHIRE = register(
            ModItemIds.SAPPHIRE,
            Item::new,
            new Item.Properties()
    );

    public static final Item SAPPHIRE_CHIP = register(
            ModItemIds.SAPPHIRE_CHIP,
            properties -> new SphereItem(
                    properties,
                    40,
                    "sapphire_chip"
            ),
            sphereProperties()
    );

    public static final Item SAPPHIRE_MARK = register(
            ModItemIds.SAPPHIRE_MARK,
            properties -> new SphereItem(
                    properties,
                    200,
                    "sapphire_mark"
            ),
            sphereProperties()
    );

    public static final Item SAPPHIRE_BROAM = register(
            ModItemIds.SAPPHIRE_BROAM,
            properties -> new SphereItem(
                    properties,
                    800,
                    "sapphire_broam"
            ),
            sphereProperties()
    );

    private static Item.Properties sphereProperties() {
        return new Item.Properties()
                .stacksTo(1)
                .component(
                        ModComponents.STORMLIGHT_CHARGE,
                        0
                )
                .component(
                        ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                        -1L
                );
    }

    private static Item register(
            ResourceKey<Item> itemKey,
            Function<Item.Properties, Item> itemFactory,
            Item.Properties properties
    ) {
        Item item = itemFactory.apply(properties.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> {
                    entries.accept(CHISEL);
                    entries.accept(DIAMOND_CHISEL);
                    entries.accept(CHIP_MOULD);
                    entries.accept(MARK_MOULD);
                    entries.accept(BROAM_MOULD);
                });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> {
                    entries.accept(SPHERE_POUCH);
                    entries.accept(MYSTERIOUS_BOOK);
                    entries.accept(WORDS_OF_RADIANCE);
                    entries.accept(WAY_OF_KINGS);

                    entries.accept(RAW_DIAMOND_CHIP);
                    entries.accept(RAW_DIAMOND_MARK);
                    entries.accept(RAW_DIAMOND_BROAM);
                    entries.accept(RAW_GARNET_CHIP);
                    entries.accept(RAW_GARNET_MARK);
                    entries.accept(RAW_GARNET_BROAM);
                    entries.accept(RAW_RUBY_CHIP);
                    entries.accept(RAW_RUBY_MARK);
                    entries.accept(RAW_RUBY_BROAM);
                    entries.accept(RAW_SAPPHIRE_CHIP);
                    entries.accept(RAW_SAPPHIRE_MARK);
                    entries.accept(RAW_SAPPHIRE_BROAM);
                    entries.accept(RAW_EMERALD_CHIP);
                    entries.accept(RAW_EMERALD_MARK);
                    entries.accept(RAW_EMERALD_BROAM);

                    entries.accept(LARGE_DIAMOND);
                    entries.accept(LARGE_GARNET);
                    entries.accept(LARGE_RUBY);
                    entries.accept(LARGE_SAPPHIRE);
                    entries.accept(LARGE_EMERALD);

                    entries.accept(DIAMOND_CHIP);
                    entries.accept(DIAMOND_MARK);
                    entries.accept(DIAMOND_BROAM);

                    entries.accept(EMERALD_CHIP);
                    entries.accept(EMERALD_MARK);
                    entries.accept(EMERALD_BROAM);

                    entries.accept(GARNET);
                    entries.accept(GARNET_CHIP);
                    entries.accept(GARNET_MARK);
                    entries.accept(GARNET_BROAM);

                    entries.accept(RUBY);
                    entries.accept(RUBY_CHIP);
                    entries.accept(RUBY_MARK);
                    entries.accept(RUBY_BROAM);

                    entries.accept(SAPPHIRE);
                    entries.accept(SAPPHIRE_CHIP);
                    entries.accept(SAPPHIRE_MARK);
                    entries.accept(SAPPHIRE_BROAM);
                });
    }
}

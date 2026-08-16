package com.scrotey.stormlight.item;
import com.scrotey.stormlight.component.ModComponents;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static final Item SPHERE_POUCH = register(
            ModItemIds.SPHERE_POUCH,
            SpherePouchItem::new,
            new Item.Properties()
                    .stacksTo(1)
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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> {
                    entries.accept(SPHERE_POUCH);
                    entries.accept(WORDS_OF_RADIANCE);

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

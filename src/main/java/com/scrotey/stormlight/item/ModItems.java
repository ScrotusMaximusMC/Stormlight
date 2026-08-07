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

    private static Item.Properties sphereProperties() {
        return new Item.Properties()
                .stacksTo(1)
                .component(ModComponents.STORMLIGHT_CHARGE, 0);
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
                    entries.accept(DIAMOND_CHIP);
                    entries.accept(DIAMOND_MARK);
                    entries.accept(DIAMOND_BROAM);
                    entries.accept(EMERALD_CHIP);
                    entries.accept(EMERALD_MARK);
                    entries.accept(EMERALD_BROAM);
                });
    }
}

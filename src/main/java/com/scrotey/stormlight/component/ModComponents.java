package com.scrotey.stormlight.component;

import com.scrotey.stormlight.Stormlight;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ItemContainerContents;

public class ModComponents {
    public static final DataComponentType<Integer> STORMLIGHT_CHARGE =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Stormlight.MOD_ID,
                            "stormlight_charge"
                    ),

                    DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 1000))
                            .build()
            );

    public static final DataComponentType<Long>
            STORMLIGHT_LAST_DECAY_TICK =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Stormlight.MOD_ID,
                            "stormlight_last_decay_tick"
                    ),
                    DataComponentType.<Long>builder()
                            .persistent(Codec.LONG)
                            .build()
            );

    public static final DataComponentType<ItemContainerContents> SPHERE_POUCH_CONTENTS =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Stormlight.MOD_ID,
                            "sphere_pouch_contents"
                    ),
                    DataComponentType
                            .<ItemContainerContents>builder()
                            .persistent(ItemContainerContents.CODEC)
                            .networkSynchronized(
                                    ItemContainerContents.STREAM_CODEC
                            )
                            .build()
            );

    public static void initialize() {
        // Loads and registers this class's components.
    }
}
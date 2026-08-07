package com.scrotey.stormlight.component;

import com.scrotey.stormlight.Stormlight;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ModComponents {
    public static final DataComponentType<Integer> STORMLIGHT_CHARGE =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Stormlight.MOD_ID,
                            "stormlight_charge"
                    ),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 200))
                            .build()
            );

    public static void initialize() {
        // Loads and registers this class's components.
    }
}
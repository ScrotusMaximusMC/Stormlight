package com.scrotey.stormlight.worldgen;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.worldgen.feature.ChrysalisFeature;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ModFeatures {

    public static final Feature<NoneFeatureConfiguration> CHRYSALIS =
            Registry.register(
                    BuiltInRegistries.FEATURE,
                    Stormlight.id("chrysalis"),
                    new ChrysalisFeature(
                            NoneFeatureConfiguration.CODEC
                    )
            );

    private ModFeatures() {
    }

    public static void initialize() {
        // Forces static registration to occur during mod startup.
    }
}
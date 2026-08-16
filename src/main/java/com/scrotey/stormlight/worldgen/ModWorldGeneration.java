package com.scrotey.stormlight.worldgen;

import com.scrotey.stormlight.Stormlight;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldGeneration {

    private static final ResourceKey<PlacedFeature> GARNET_ORE =
            placedFeatureKey("garnet_ore");

    private static final ResourceKey<PlacedFeature> RUBY_ORE =
            placedFeatureKey("ruby_ore");

    private static final ResourceKey<PlacedFeature> SAPPHIRE_ORE =
            placedFeatureKey("sapphire_ore");


    private ModWorldGeneration() {
    }

    public static void initialize() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                GARNET_ORE
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                RUBY_ORE
        );

        BiomeModifications.addFeature(
                BiomeSelectors.tag(
                        BiomeTags.IS_MOUNTAIN
                ),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                SAPPHIRE_ORE
        );


    }

    private static ResourceKey<PlacedFeature> placedFeatureKey(
            String path
    ) {
        return ResourceKey.create(
                Registries.PLACED_FEATURE,
                Stormlight.id(path)
        );
    }
}
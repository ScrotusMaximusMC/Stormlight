package com.scrotey.stormlight.worldgen;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.worldgen.biome.ModBiomes;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldGeneration {

    private static final ResourceKey<PlacedFeature> GARNET_ORE =
            placedFeatureKey("garnet_ore");

    private static final ResourceKey<PlacedFeature> RUBY_ORE =
            placedFeatureKey("ruby_ore");

    private static final ResourceKey<PlacedFeature> SAPPHIRE_ORE =
            placedFeatureKey("sapphire_ore");

    private static final ResourceKey<PlacedFeature> CHRYSALIS =
            placedFeatureKey("chrysalis");

    private static final ResourceKey<PlacedFeature> SHATTERED_PLAINS_SURFACE =
            placedFeatureKey("shattered_plains_surface");

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

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        ModBiomes.SHATTERED_PLAINS
                ),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                SHATTERED_PLAINS_SURFACE
        );

        /*
         * Temporary home for chrysalises.
         *
         * Once the Shattered Plains biome exists, this selector is
         * the bit we'll replace.
         */
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        Biomes.BADLANDS,
                        Biomes.ERODED_BADLANDS,
                        Biomes.WOODED_BADLANDS,
                        ModBiomes.SHATTERED_PLAINS
                ),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                CHRYSALIS
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
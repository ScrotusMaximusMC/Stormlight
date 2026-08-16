package com.scrotey.stormlight.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

import static terrablender.api.ParameterUtils.*;

public final class ShatteredPlainsRegion extends Region {

    public ShatteredPlainsRegion(Identifier name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(
            Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper
    ) {
        VanillaParameterOverlayBuilder builder =
                new VanillaParameterOverlayBuilder();

        /*
         * Region-scale Shattered Plains.
         *
         * TerraBlender's region weight remains at 1 (the minimum), which is
         * what makes encounters uncommon. Once that rare region is selected,
         * however, almost every inland surface climate point within the region
         * becomes Shattered Plains.
         *
         * This deliberately separates rarity from size:
         *   - rare REGION selection
         *   - very large BIOME coverage inside that selected region
         *
         * Temperature/humidity here are placement parameters only. The actual
         * Shattered Plains biome remains hot, dry and precipitation-free via
         * its biome JSON.
         */
        new ParameterPointListBuilder()
                .temperature(
                        Temperature.COOL,
                        Temperature.NEUTRAL,
                        Temperature.WARM,
                        Temperature.HOT
                )
                .humidity(
                        Humidity.ARID,
                        Humidity.DRY,
                        Humidity.NEUTRAL,
                        Humidity.WET,
                        Humidity.HUMID
                )
                .continentalness(
                        Continentalness.MID_INLAND,
                        Continentalness.FAR_INLAND
                )
                .erosion(Erosion.FULL_RANGE)
                .depth(Depth.SURFACE)
                .weirdness(Weirdness.FULL_RANGE)
                .build()
                .forEach(point ->
                        builder.add(
                                point,
                                ModBiomes.SHATTERED_PLAINS
                        )
                );

        builder.build().forEach(mapper);
    }
}

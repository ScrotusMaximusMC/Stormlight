package com.scrotey.stormlight.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import org.jspecify.annotations.Nullable;

/* Implemented on MultiNoiseBiomeSourceParameterList by a mixin. */
public interface ShatteredPlainsParameters {

    /* The Shattered Plains biome, or null if this isn't the overworld preset. */
    @Nullable Holder<Biome> stormlight$shatteredPlains();
}

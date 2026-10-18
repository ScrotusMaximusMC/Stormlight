package com.scrotey.stormlight.worldgen.biome;

import net.minecraft.world.level.levelgen.DensityFunction;

import org.jspecify.annotations.Nullable;

/* Implemented on RandomState by a mixin. */
public interface ShatteredPlainsRandomState {

    /* The wired Shattered Plains terrain weight, or null if this isn't an overworld. */
    @Nullable DensityFunction stormlight$plainsWeight();
}

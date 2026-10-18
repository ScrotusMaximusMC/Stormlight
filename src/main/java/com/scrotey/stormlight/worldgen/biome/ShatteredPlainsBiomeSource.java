package com.scrotey.stormlight.worldgen.biome;

import net.minecraft.world.level.levelgen.RandomState;

/* Implemented on MultiNoiseBiomeSource by a mixin. */
public interface ShatteredPlainsBiomeSource {

    /*
     * Seeds the Shattered Plains mask from the level's RandomState. Until
     * this is called (and for any non-overworld preset) the source behaves
     * exactly like vanilla.
     */
    void stormlight$attachShatteredPlains(RandomState randomState);
}

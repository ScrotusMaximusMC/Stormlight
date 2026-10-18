package com.scrotey.stormlight.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsRandomState;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsTerrain;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/* Keeps sea-level water out of the Shattered Plains chasms. */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Aquifer.FluidPicker stormlight$dryChasms(
            Aquifer.FluidPicker picker,
            @Local(argsOnly = true) RandomState randomState
    ) {
        DensityFunction weight = ((ShatteredPlainsRandomState) (Object) randomState).stormlight$plainsWeight();
        return weight == null ? picker : ShatteredPlainsTerrain.dryChasms(picker, weight);
    }
}

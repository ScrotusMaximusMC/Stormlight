package com.scrotey.stormlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsRandomState;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsTerrain;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/* Lets the Shattered Plains terrain wrap vanilla's height functions while the router is wired. */
@Mixin(RandomState.class)
public abstract class RandomStateMixin implements ShatteredPlainsRandomState {
    @Unique
    private @Nullable DensityFunction stormlight$plainsWeight;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
            )
    )
    private NoiseRouter stormlight$wireShatteredPlains(
            NoiseRouter router,
            DensityFunction.Visitor visitor,
            Operation<NoiseRouter> original
    ) {
        ShatteredPlainsTerrain.beginWiring((RandomState) (Object) this, router);
        NoiseRouter wired;
        try {
            wired = original.call(router, visitor);
        } catch (RuntimeException e) {
            ShatteredPlainsTerrain.endWiring(null);
            throw e;
        }
        stormlight$plainsWeight = ShatteredPlainsTerrain.endWiring(visitor);
        return wired;
    }

    @Override
    public @Nullable DensityFunction stormlight$plainsWeight() {
        return stormlight$plainsWeight;
    }
}

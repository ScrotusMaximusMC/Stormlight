package com.scrotey.stormlight.mixin;

import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsTerrain;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 * Mapping a holder inlines it and drops its registry key, so the first
 * mapping (RandomState wiring) is the only chance to recognise vanilla's
 * named terrain functions and wrap them.
 */
@Mixin(DensityFunctions.HolderHolder.class)
public abstract class DensityFunctionsHolderHolderMixin {
    @Shadow
    @Final
    private Holder<DensityFunction> function;

    @Inject(method = "mapChildren", at = @At("HEAD"), cancellable = true)
    private void stormlight$wrapShatteredPlains(
            DensityFunction.Visitor visitor,
            CallbackInfoReturnable<DensityFunction> callback
    ) {
        DensityFunction wrapped = ShatteredPlainsTerrain.wrap(function);
        if (wrapped != null) {
            callback.setReturnValue(wrapped.mapChildren(visitor));
        }
    }
}

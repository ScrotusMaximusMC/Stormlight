package com.scrotey.stormlight.mixin;

import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsBiomeSource;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * The level's RandomState is created here, so this is the first point the
 * Shattered Plains mask can be seeded. It runs before any chunk (or the
 * spawn point) is generated.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapShatteredPlainsMixin {
    @Shadow
    @Final
    private RandomState randomState;

    @Shadow
    protected abstract ChunkGenerator generator();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void stormlight$attachShatteredPlains(CallbackInfo callback) {
        if (generator().getBiomeSource() instanceof ShatteredPlainsBiomeSource source) {
            source.stormlight$attachShatteredPlains(randomState);
        }
    }
}

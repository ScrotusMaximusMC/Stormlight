package com.scrotey.stormlight.worldgen.biome;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    public static final ResourceKey<Biome> SHATTERED_PLAINS =
            ResourceKey.create(
                    Registries.BIOME,
                    Stormlight.id("shattered_plains")
            );

    private ModBiomes() {
    }
}

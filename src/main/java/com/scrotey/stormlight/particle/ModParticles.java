package com.scrotey.stormlight.particle;

import com.scrotey.stormlight.Stormlight;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
    public static final SimpleParticleType WINDSPREN =
            Registry.register(
                    BuiltInRegistries.PARTICLE_TYPE,
                    Stormlight.id("windspren"),
                    FabricParticleTypes.simple()
            );

    public static final SimpleParticleType SURGE_LIGHT =
            Registry.register(
                    BuiltInRegistries.PARTICLE_TYPE,
                    Stormlight.id("surge_light"),
                    FabricParticleTypes.simple()
            );

    public static final SimpleParticleType HIGHSTORM_LEAF =
            Registry.register(
                    BuiltInRegistries.PARTICLE_TYPE,
                    Stormlight.id("highstorm_leaf"),
                    FabricParticleTypes.simple()
            );


    public static final SimpleParticleType HONORSPREN_MOTE =
            Registry.register(
                    BuiltInRegistries.PARTICLE_TYPE,
                    Stormlight.id("honorspren_mote"),
                    FabricParticleTypes.simple()
            );

    private ModParticles() {
    }

    public static void initialize() {
        // Loads the class and registers the particle.
    }
}

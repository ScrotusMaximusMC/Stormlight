package com.scrotey.stormlight.client.spren;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/** Long-lived, gently fading motes left along the spren's flight. */
public final class SprenSeekTrailParticle extends SingleQuadParticle {
    private SprenSeekTrailParticle(ClientLevel level,
            double x, double y, double z,
            TextureAtlasSprite sprite) {
        super(level, x, y, z, 0, 0, 0, sprite);
        lifetime = 20 * 35;
        gravity = 0;
        hasPhysics = false;
        quadSize = 0.20F;
        setColor(0.72F, 0.98F, 1.0F);
        alpha = 1.0F;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float fadeStart = lifetime * 0.55F;
        alpha = age < fadeStart ? 1.0F
                : Math.max(0.0F,
                        (lifetime - age) / (lifetime - fadeStart));
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 0x00F000F0;
    }

    @Override
    public Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed,
                net.minecraft.util.RandomSource random) {
            return new SprenSeekTrailParticle(level, x, y, z,
                    sprites.get(random));
        }
    }
}

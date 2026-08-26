package com.scrotey.stormlight.client.spren;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Tiny, full-bright honorspren twinkle.
 */
public final class HonorSprenMoteParticle extends SingleQuadParticle {
    private final float baseSize;

    private HonorSprenMoteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);

        this.lifetime = 10 + this.random.nextInt(5);
        this.gravity = 0.0F;
        this.friction = 0.88F;
        this.hasPhysics = false;

        this.baseSize = 0.14F
                + this.random.nextFloat() * 0.035F;
        this.quadSize = this.baseSize;

        this.xd = (this.random.nextDouble() - 0.5) * 0.004;
        this.yd = (this.random.nextDouble() - 0.5) * 0.004;
        this.zd = (this.random.nextDouble() - 0.5) * 0.004;

        this.setColor(0.82F, 0.97F, 1.0F);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float life = this.age / (float) this.lifetime;

        // Smoothly fade in and out instead of appearing/disappearing like a
        // popping bubble. sin(0..PI) gives zero alpha at both ends and a soft
        // luminous peak through the middle.
        float glow = (float) Math.sin(life * Math.PI);
        this.alpha = glow;

        // Keep the size almost constant; only a tiny breathing twinkle remains.
        this.quadSize = this.baseSize * (0.94F + glow * 0.06F);

        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
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
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                net.minecraft.util.RandomSource random
        ) {
            return new HonorSprenMoteParticle(
                    level,
                    x,
                    y,
                    z,
                    xSpeed,
                    ySpeed,
                    zSpeed,
                    sprites.get(random)
            );
        }
    }
}

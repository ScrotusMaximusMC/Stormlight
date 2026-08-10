package com.scrotey.stormlight.client.highstorm;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A leaf caught in the Highstorm's westward gale.
 *
 * Vanilla falling leaves choose their own gentle motion and ignore the
 * velocity passed to them. This particle keeps that fluttering appearance,
 * but carries the leaf rapidly west and adds irregular cross-wind gusts.
 */
public final class HighstormLeafParticle extends SingleQuadParticle {
    private final float flutterOffset;
    private final float flutterSpeed;
    private final double sidewaysStrength;
    private final double verticalStrength;

    private HighstormLeafParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            TextureAtlasSprite sprite
    ) {
        super(
                level,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed,
                sprite
        );

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.lifetime = 55 + this.random.nextInt(55);
        this.gravity = 0.012F;
        this.friction = 0.985F;
        this.hasPhysics = true;

        this.quadSize = 0.10F
                + this.random.nextFloat() * 0.09F;
        this.setSize(0.16F, 0.16F);

        // The vanilla leaf sprites are greyscale and intended to be tinted.
        float shade = 0.78F
                + this.random.nextFloat() * 0.22F;

        this.setColor(
                0.38F * shade,
                0.62F * shade,
                0.20F * shade
        );

        this.flutterOffset =
                this.random.nextFloat() * Mth.TWO_PI;

        this.flutterSpeed =
                0.22F + this.random.nextFloat() * 0.18F;

        this.sidewaysStrength =
                0.025 + this.random.nextDouble() * 0.035;

        this.verticalStrength =
                0.012 + this.random.nextDouble() * 0.025;

        this.roll =
                this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float flutter =
                this.age * this.flutterSpeed
                        + this.flutterOffset;

        // Strong overall westward movement with irregular gusts.
        this.xd -= 0.010
                + this.random.nextDouble() * 0.006;

        this.zd += Math.sin(flutter)
                * this.sidewaysStrength;

        this.yd += Math.cos(flutter * 0.73F)
                * this.verticalStrength;

        if (this.random.nextFloat() < 0.14F) {
            this.zd +=
                    (this.random.nextDouble() - 0.5)
                            * 0.13;

            this.yd +=
                    (this.random.nextDouble() - 0.45)
                            * 0.08;
        }

        this.yd -= this.gravity;

        this.roll += 0.24F
                + (float) Math.abs(this.zd) * 0.18F;

        this.move(this.xd, this.yd, this.zd);

        if (this.onGround) {
            this.remove();
            return;
        }

        this.xd *= this.friction;
        this.yd *= 0.965F;
        this.zd *= 0.94F;
    }

    @Override
    public Layer getLayer() {
        return Layer.OPAQUE;
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
            return new HighstormLeafParticle(
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
package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;


public final class HighstormAtmosphere {

    // Highstorms blow due west (negative X).
    private static final double WIND_X = -0.085;
    private static final double WIND_Z = 0.0;

    // Sparse v2-style blue wisps, now spawning in a full circle around players.
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int WINDSPREN_PER_BURST = 7;
    private static final double WINDSPREN_MIN_DISTANCE = 2.0;
    private static final double WINDSPREN_MAX_DISTANCE = 48.0;

    // Strong westward current with gentle firefly-like wandering.
    private static final double WINDSPREN_WEST_SPEED = -0.9;
    private static final double WINDSPREN_VERTICAL_WISP = 0.070;
    private static final double WINDSPREN_SIDEWAYS_WISP = 0.11;

    // Actual lightning. Each check has a chance to create one real strike.
    private static final int LIGHTNING_CHECK_INTERVAL_TICKS = 24;
    private static final float LIGHTNING_CHANCE_PER_CHECK = 0.38F;
    private static final double LIGHTNING_MIN_DISTANCE = 8.0;
    private static final double LIGHTNING_MAX_DISTANCE = 30.0;

    private static final int WIND_INTERVAL_TICKS = 10;

    // Wither I is refreshed outdoors every two seconds during the active storm.
    // Its short duration lets the effect expire soon after reaching shelter.
    private static final int WITHER_INTERVAL_TICKS = 40;
    private static final int WITHER_DURATION_TICKS = 100;
    private static final int WITHER_AMPLIFIER = 0;

    /*
     * The Elytra sound is a short wind sample rather than a continuous loop.
     * Replaying the main layer once per second prevents audible gaps, while a
     * slower, lower-pitched layer gives the active storm some weight.
     */
    private static final int HIGHSTORM_WIND_INTERVAL_TICKS = 20;
    private static final int HIGHSTORM_DEEP_GUST_INTERVAL_TICKS = 65;

    /*
     * Sparse, drawn-out gusts announce the approaching wall. Departure gusts
     * are slightly closer together so the gale fades away rather than ending
     * abruptly at the HIGHSTORM -> PASSING transition.
     */
    private static final int APPROACH_GUST_INTERVAL_TICKS = 120;
    private static final int PASSING_GUST_INTERVAL_TICKS = 90;

    private static final int THUNDER_SOUND_INTERVAL_TICKS = 75;
    private static final int RUMBLE_SOUND_INTERVAL_TICKS = 90;
    private static final int WARNING_RUMBLE_INTERVAL_TICKS = 180;

    private HighstormAtmosphere() {
    }

    public static void tick(
            ServerLevel level,
            HighstormPhase phase,
            long ticksRemaining
    ) {
        float intensity = getIntensity(phase, ticksRemaining);

        if (intensity <= 0.0F) {
            return;
        }

        long gameTime = level.getGameTime();

        if (gameTime % PARTICLE_INTERVAL_TICKS == 0) {
            spawnWindParticles(level, intensity);
        }

        if (gameTime % WIND_INTERVAL_TICKS == 0) {
            pushExposedEntities(level, intensity);
        }

        if (phase == HighstormPhase.HIGHSTORM
                && gameTime % WITHER_INTERVAL_TICKS == 0) {
            witherExposedPlayers(level);
        }

        if (phase == HighstormPhase.HIGHSTORM) {
            if (gameTime % LIGHTNING_CHECK_INTERVAL_TICKS == 0) {
                spawnRealLightning(level);
            }

            if (gameTime % HIGHSTORM_WIND_INTERVAL_TICKS == 0) {
                playHighstormWindSounds(level);
            }

            if (gameTime
                    % HIGHSTORM_DEEP_GUST_INTERVAL_TICKS == 0) {
                playHighstormDeepGustSounds(level);
            }

            if (gameTime % THUNDER_SOUND_INTERVAL_TICKS == 0) {
                playThunderSounds(level);
            }

            if (gameTime % RUMBLE_SOUND_INTERVAL_TICKS == 0) {
                playRumbleSounds(level);
            }
        } else {
            int gustInterval = phase == HighstormPhase.APPROACHING
                    ? APPROACH_GUST_INTERVAL_TICKS
                    : PASSING_GUST_INTERVAL_TICKS;

            if (gameTime % gustInterval == 0) {
                playDistantGustSounds(level, phase, intensity);
            }

            if (gameTime % WARNING_RUMBLE_INTERVAL_TICKS == 0) {
                playWarningRumbleSounds(level, intensity);
            }
        }
    }

    private static float getIntensity(
            HighstormPhase phase,
            long ticksRemaining
    ) {
        return switch (phase) {
            case CALM -> 0.0F;
            case APPROACHING -> clamp(
                    1.0F - ticksRemaining
                            / (float) HighstormTimings.APPROACHING_TICKS,
                    0.15F,
                    0.75F
            );
            case HIGHSTORM -> 1.0F;
            case PASSING -> clamp(
                    ticksRemaining / (float) HighstormTimings.PASSING_TICKS,
                    0.10F,
                    0.65F
            );
        };
    }

    private static void spawnWindParticles(
            ServerLevel level,
            float intensity
    ) {
        for (ServerPlayer player : level.players()) {
            if (!isExposed(level, player)) {
                continue;
            }

            int count = Math.max(
                    1,
                    Math.round(WINDSPREN_PER_BURST * intensity)
            );

            for (int i = 0; i < count; i++) {
                double angle = level.getRandom().nextDouble()
                        * Math.PI * 2.0;

                double distance = WINDSPREN_MIN_DISTANCE
                        + level.getRandom().nextDouble()
                        * (WINDSPREN_MAX_DISTANCE
                        - WINDSPREN_MIN_DISTANCE);

                double x = player.getX()
                        + Math.cos(angle) * distance;

                double y = player.getY() - 0.5
                        + level.getRandom().nextDouble() * 7.0;

                double z = player.getZ()
                        + Math.sin(angle) * distance;

                double westSpeed = WINDSPREN_WEST_SPEED
                        * (0.80
                        + level.getRandom().nextDouble() * 0.40)
                        * intensity;

                double verticalWisp =
                        (level.getRandom().nextDouble() - 0.5)
                                * WINDSPREN_VERTICAL_WISP;

                double sidewaysWisp =
                        (level.getRandom().nextDouble() - 0.5)
                                * WINDSPREN_SIDEWAYS_WISP;

                level.sendParticles(
                        ModParticles.WINDSPREN,
                        x,
                        y,
                        z,
                        0,
                        westSpeed,
                        verticalWisp,
                        sidewaysWisp,
                        1.0
                );
            }
        }
    }

    private static void spawnRealLightning(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (level.getRandom().nextFloat()
                    > LIGHTNING_CHANCE_PER_CHECK) {
                continue;
            }

            double angle = level.getRandom().nextDouble()
                    * Math.PI * 2.0;
            double distance = LIGHTNING_MIN_DISTANCE
                    + level.getRandom().nextDouble()
                    * (LIGHTNING_MAX_DISTANCE - LIGHTNING_MIN_DISTANCE);

            int x = Mth.floor(
                    player.getX() + Math.cos(angle) * distance
            );
            int z = Mth.floor(
                    player.getZ() + Math.sin(angle) * distance
            );
            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING,
                    x,
                    z
            );

            LightningBolt lightning = new LightningBolt(
                    EntityTypes.LIGHTNING_BOLT,
                    level
            );
            lightning.setPos(x + 0.5, y, z + 0.5);
            level.addFreshEntity(lightning);
        }
    }

    private static void pushExposedEntities(
            ServerLevel level,
            float intensity
    ) {
        for (LivingEntity entity : level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                entity -> entity.isAlive()
                        && isExposed(level, entity)
        )) {
            if (entity instanceof ServerPlayer player
                    && (player.isCreative()
                    || player.isSpectator())) {
                continue;
            }

            double resistance = entity.isShiftKeyDown()
                    ? 0.35
                    : 1.0;

            double airborneMultiplier = entity.onGround()
                    ? 1.0
                    : 1.45;

            boolean strongGust = level.getRandom()
                    .nextFloat() < 0.12F * intensity;

            double gustMultiplier = strongGust ? 2.6 : 1.0;
            double strength = intensity
                    * resistance
                    * airborneMultiplier
                    * gustMultiplier;

            entity.push(
                    WIND_X * strength,
                    strongGust ? 0.045 * intensity : 0.0,
                    WIND_Z * strength
            );
        }
    }

    private static void witherExposedPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!isAffectedPlayer(level, player)) {
                continue;
            }

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.WITHER,
                            WITHER_DURATION_TICKS,
                            WITHER_AMPLIFIER,
                            true,
                            true,
                            true
                    )
            );
        }
    }

    private static boolean isAffectedPlayer(
            ServerLevel level,
            ServerPlayer player
    ) {
        return !player.isCreative()
                && !player.isSpectator()
                && isExposed(level, player);
    }

    private static void playHighstormWindSounds(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!isExposed(level, player)) {
                continue;
            }

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ELYTRA_FLYING,
                    SoundSource.WEATHER,
                    2.20F,
                    0.48F + level.getRandom().nextFloat() * 0.12F
            );
        }
    }

    private static void playHighstormDeepGustSounds(
            ServerLevel level
    ) {
        for (ServerPlayer player : level.players()) {
            if (!isExposed(level, player)) {
                continue;
            }

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ELYTRA_FLYING,
                    SoundSource.WEATHER,
                    1.65F,
                    0.27F + level.getRandom().nextFloat() * 0.09F
            );
        }
    }

    private static void playThunderSounds(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            float shelterMultiplier = isExposed(level, player)
                    ? 1.0F
                    : 0.55F;

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    1.9F * shelterMultiplier,
                    0.60F + level.getRandom().nextFloat() * 0.22F
            );
        }
    }

    private static void playRumbleSounds(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            float shelterMultiplier = isExposed(level, player)
                    ? 1.0F
                    : 0.65F;

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.WEATHER,
                    1.15F * shelterMultiplier,
                    0.42F + level.getRandom().nextFloat() * 0.14F
            );
        }
    }

    private static void playDistantGustSounds(
            ServerLevel level,
            HighstormPhase phase,
            float intensity
    ) {
        for (ServerPlayer player : level.players()) {
            if (!isExposed(level, player)) {
                continue;
            }

            float phaseMultiplier = phase == HighstormPhase.APPROACHING
                    ? 0.70F + intensity * 0.75F
                    : 0.45F + intensity * 0.70F;

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ELYTRA_FLYING,
                    SoundSource.WEATHER,
                    phaseMultiplier,
                    0.24F + level.getRandom().nextFloat() * 0.09F
            );
        }
    }

    private static void playWarningRumbleSounds(
            ServerLevel level,
            float intensity
    ) {
        for (ServerPlayer player : level.players()) {
            if (!isExposed(level, player)) {
                continue;
            }

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.WEATHER,
                    0.55F + intensity * 0.65F,
                    0.48F + level.getRandom().nextFloat() * 0.14F
            );
        }
    }

    private static boolean isExposed(
            ServerLevel level,
            LivingEntity entity
    ) {
        return level.canSeeSky(entity.blockPosition().above());
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum
    ) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}

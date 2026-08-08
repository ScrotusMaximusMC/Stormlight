package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;
import com.scrotey.stormlight.item.SphereItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class SphereJarChargingEffects {
    private static final int
            PARTICLE_INTERVAL_TICKS = 3;

    private static final int
            STREAKS_PER_BURST = 5;

    private static final int
            TRAIL_MIN_DURATION_TICKS = 16;

    private static final int
            TRAIL_DURATION_VARIATION = 10;

    private static final int
            SOUND_INTERVAL_TICKS = 44;

    private static final int[] CHARGING_COLOURS = {
            0xFF54DFFF,
            0xFF72EEFF,
            0xFF3AAFFF,
            0xFF527CFF,
            0xFF8FCFFF,
            0xFF9B8CFF,
            0xFFD6FAFF,
            0xFFFFD978
    };

    private SphereJarChargingEffects() {
    }

    static void tick(
            ServerLevel level,
            SphereJarBlockEntity sphereJar,
            long elapsedHighstormTicks
    ) {
        /*
         * This is evaluated after the current tick's charging, so both
         * particles and sound stop immediately when all spheres are full.
         */
        if (!hasSphereNeedingCharge(
                sphereJar
        )) {
            return;
        }

        BlockPos jarPosition =
                sphereJar.getBlockPos();

        RandomSource random =
                level.getRandom();

        if (elapsedHighstormTicks
                % PARTICLE_INTERVAL_TICKS
                == 0L) {

            spawnChargingStreaks(
                    level,
                    jarPosition,
                    random
            );
        }

        long soundOffset =
                Math.floorMod(
                        jarPosition.asLong(),
                        (long) SOUND_INTERVAL_TICKS
                );

        if ((elapsedHighstormTicks
                + soundOffset)
                % SOUND_INTERVAL_TICKS
                == 0L) {

            level.playSound(
                    null,
                    jarPosition,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    6.90F,
                    0.85F
                            + random.nextFloat()
                            * 0.15F
            );
        }
    }

    private static boolean hasSphereNeedingCharge(
            SphereJarBlockEntity sphereJar
    ) {
        for (int slot = 0;
             slot < sphereJar.getContainerSize();
             slot++) {

            ItemStack stack =
                    sphereJar.getItem(slot);

            if (!(stack.getItem()
                    instanceof SphereItem sphere)) {

                continue;
            }

            if (sphere.getCharge(stack)
                    < sphere.getCapacity()) {

                return true;
            }
        }

        return false;
    }

    private static void spawnChargingStreaks(
            ServerLevel level,
            BlockPos jarPosition,
            RandomSource random
    ) {
        Vec3 target =
                new Vec3(
                        jarPosition.getX() + 0.5,
                        jarPosition.getY() + 0.48,
                        jarPosition.getZ() + 0.5
                );

        int colourOffset =
                random.nextInt(
                        CHARGING_COLOURS.length
                );

        int streaksFromAbove =
                (STREAKS_PER_BURST + 1)
                        / 2;

        for (int streak = 0;
             streak < STREAKS_PER_BURST;
             streak++) {

            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0;

            double startX;
            double startY;
            double startZ;

            if (streak < streaksFromAbove) {
                double horizontalSpread =
                        0.15
                                + random.nextDouble()
                                * 1.10;

                startX =
                        jarPosition.getX()
                                + 0.5
                                + Math.cos(angle)
                                * horizontalSpread;

                startY =
                        jarPosition.getY()
                                + 2.70
                                + random.nextDouble()
                                * 1.30;

                startZ =
                        jarPosition.getZ()
                                + 0.5
                                + Math.sin(angle)
                                * horizontalSpread;
            } else {
                double radius =
                        2.0
                                + random.nextDouble()
                                * 1.5;

                startX =
                        jarPosition.getX()
                                + 0.5
                                + Math.cos(angle)
                                * radius;

                startY =
                        jarPosition.getY()
                                + 0.15
                                + random.nextDouble()
                                * 1.85;

                startZ =
                        jarPosition.getZ()
                                + 0.5
                                + Math.sin(angle)
                                * radius;
            }

            int colour =
                    CHARGING_COLOURS[
                            (colourOffset + streak)
                                    % CHARGING_COLOURS.length
                            ];

            int duration =
                    TRAIL_MIN_DURATION_TICKS
                            + random.nextInt(
                            TRAIL_DURATION_VARIATION
                    );

            TrailParticleOption trail =
                    new TrailParticleOption(
                            target,
                            colour,
                            duration
                    );

            level.sendParticles(
                    trail,
                    startX,
                    startY,
                    startZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }
}
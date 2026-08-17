package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.item.ModItems;

import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class MysteriousBookRitual {
    private static final int TICKS_PER_SECOND = 20;
    private static final int CHARGE_TICKS = 60 * TICKS_PER_SECOND;
    private static final int LIGHTNING_STRIKES = 10;
    private static final int LIGHTNING_INTERVAL_TICKS = 2;
    private static final int PARTICLE_INTERVAL_TICKS = 3;
    private static final int STREAKS_PER_BURST = 6;

    private static final int[] CHARGING_COLOURS = {
            0xFFF4FEFF,
            0xFFDDF8FF,
            0xFFC8F0FF,
            0xFFAFE6FF,
            0xFF8ED8FF,
            0xFF6EC8FF,
            0xFF4EB7FF,
            0xFF9EE8FF
    };
    private static final Map<UUID, RitualState> ACTIVE = new HashMap<>();

    private MysteriousBookRitual() {
    }

    static void tick(ServerLevel level, boolean highstormActive) {
        if (!highstormActive) {
            clearBookGlow(level);
            ACTIVE.clear();
            return;
        }

        Set<UUID> seen = new HashSet<>();

        for (ItemEntity itemEntity : level.getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                entity -> entity.getItem().is(ModItems.MYSTERIOUS_BOOK)
        )) {
            UUID id = itemEntity.getUUID();
            seen.add(id);

            if (!level.canSeeSky(itemEntity.blockPosition())) {
                itemEntity.setGlowingTag(false);
                ACTIVE.remove(id);
                continue;
            }

            RitualState state = ACTIVE.computeIfAbsent(id, ignored -> new RitualState());
            itemEntity.setGlowingTag(true);
            state.exposureTicks++;

            if (state.exposureTicks < CHARGE_TICKS) {
                if (state.exposureTicks % PARTICLE_INTERVAL_TICKS == 0) {
                    spawnChargingStreaks(level, itemEntity);
                }

                if (state.exposureTicks % 46 == 0) {
                    level.playSound(
                            null,
                            itemEntity.blockPosition(),
                            SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.WEATHER,
                            5.0F,
                            0.72F + level.getRandom().nextFloat() * 0.18F
                    );
                }
                continue;
            }

            state.lightningDelayTicks++;
            if (state.lightningDelayTicks < LIGHTNING_INTERVAL_TICKS) {
                continue;
            }
            state.lightningDelayTicks = 0;

            strike(level, itemEntity);
            state.lightningStrikes++;

            if (state.lightningStrikes >= LIGHTNING_STRIKES) {
                transform(level, itemEntity);
                ACTIVE.remove(id);
            }
        }

        ACTIVE.keySet().removeIf(id -> !seen.contains(id));
    }

    private static void clearBookGlow(ServerLevel level) {
        for (ItemEntity itemEntity : level.getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                entity -> entity.getItem().is(ModItems.MYSTERIOUS_BOOK)
        )) {
            itemEntity.setGlowingTag(false);
        }
    }

    private static void strike(ServerLevel level, ItemEntity itemEntity) {
        LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        lightning.setPos(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        lightning.setVisualOnly(true);
        level.addFreshEntity(lightning);
    }

    private static void transform(ServerLevel level, ItemEntity oldBook) {
        double x = oldBook.getX();
        double y = oldBook.getY();
        double z = oldBook.getZ();

        ServerPlayer recipient = nearestPlayer(level, oldBook, 32.0);

        oldBook.discard();

        ItemEntity transformed = new ItemEntity(
                level,
                x,
                y,
                z,
                new ItemStack(ModItems.WORDS_OF_RADIANCE)
        );
        transformed.setDeltaMovement(0.0, 0.12, 0.0);
        level.addFreshEntity(transformed);

        level.playSound(
                null,
                x, y, z,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.WEATHER,
                6.0F,
                0.72F
        );

        if (recipient != null) {
            var advancement = level.getServer()
                    .getAdvancements()
                    .get(com.scrotey.stormlight.Stormlight.id("a_surgebinder_awakes"));

            if (advancement != null) {
                recipient.getAdvancements().award(advancement, "awakened");
            }
        }
    }

    private static ServerPlayer nearestPlayer(
            ServerLevel level,
            ItemEntity itemEntity,
            double maxDistance
    ) {
        ServerPlayer nearest = null;
        double bestDistance = maxDistance * maxDistance;

        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(itemEntity);
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private static void spawnChargingStreaks(ServerLevel level, ItemEntity book) {
        RandomSource random = level.getRandom();
        Vec3 target = new Vec3(book.getX(), book.getY() + 0.18, book.getZ());
        int colourOffset = random.nextInt(CHARGING_COLOURS.length);

        for (int streak = 0; streak < STREAKS_PER_BURST; streak++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 1.8 + random.nextDouble() * 2.0;

            double startX = book.getX() + Math.cos(angle) * radius;
            double startY = book.getY() + 0.4 + random.nextDouble() * 3.5;
            double startZ = book.getZ() + Math.sin(angle) * radius;

            int colour = CHARGING_COLOURS[(colourOffset + streak) % CHARGING_COLOURS.length];
            int duration = 16 + random.nextInt(10);

            level.sendParticles(
                    new TrailParticleOption(target, colour, duration),
                    startX, startY, startZ,
                    1,
                    0.0, 0.0, 0.0, 0.0
            );
        }
    }

    private static final class RitualState {
        private int exposureTicks;
        private int lightningDelayTicks;
        private int lightningStrikes;
    }
}

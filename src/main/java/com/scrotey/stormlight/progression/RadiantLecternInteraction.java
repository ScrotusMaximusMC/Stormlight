package com.scrotey.stormlight.progression;

import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.network.ChooseRadiantOrderPayload;
import com.scrotey.stormlight.network.OpenRadiantProgressionPayload;
import com.scrotey.stormlight.network.UnlockRadiantLevelPayload;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

public final class RadiantLecternInteraction {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0;

    private RadiantLecternInteraction() {
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register(
                (player, level, hand, hitResult) -> {
                    BlockPos pos = hitResult.getBlockPos();

                    if (player.isSpectator()
                            || !hasWordsOfRadiance(level, pos)) {
                        return InteractionResult.PASS;
                    }

                    if (!level.isClientSide()
                            && player instanceof ServerPlayer serverPlayer) {
                        openFor(serverPlayer, pos);
                    }

                    return InteractionResult.SUCCESS;
                }
        );
    }

    public static void chooseOrder(
            ServerPlayer player,
            ChooseRadiantOrderPayload payload
    ) {
        BlockPos pos = payload.lecternPos();

        if (player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) > MAX_INTERACTION_DISTANCE_SQUARED
                || !hasWordsOfRadiance(player.level(), pos)) {
            return;
        }

        RadiantOrderRegistry.byNetworkId(payload.orderNetworkId())
                .ifPresent(order -> {
                    if (RadiantProgression.getOrder(player).isPresent()) {
                        openFor(player, pos);
                        return;
                    }

                    if (!RadiantProgression.canAffordLevel(
                            player,
                            RadiantProgression.MIN_LEVEL
                    )) {
                        sendInsufficientExperience(
                                player,
                                RadiantProgression.MIN_LEVEL
                        );
                        openFor(player, pos);
                        return;
                    }

                    if (RadiantProgression.chooseOrder(player, order)) {
                        player.sendSystemMessage(
                                Component.translatable(
                                        "message.stormlight.order.chosen",
                                        Component.translatable(
                                                order.translationKey()
                                        )
                                )
                        );
                        StormlightManager.refreshStatus(player);
                    }

                    openFor(player, pos);
                });
    }

    public static void unlockLevel(
            ServerPlayer player,
            UnlockRadiantLevelPayload payload
    ) {
        BlockPos pos = payload.lecternPos();

        if (!canUseLectern(player, pos)) {
            return;
        }

        int requestedLevel = payload.requestedLevel();
        RadiantProgression.UnlockResult result =
                RadiantProgression.unlockNextLevel(
                        player,
                        requestedLevel
                );

        if (result == RadiantProgression.UnlockResult.SUCCESS) {
            player.sendSystemMessage(
                    Component.translatable(
                            "message.stormlight.radiant.level_unlocked",
                            requestedLevel
                    )
            );
            StormlightManager.refreshStatus(player);
        } else if (result
                == RadiantProgression.UnlockResult.NOT_ENOUGH_EXPERIENCE) {
            sendInsufficientExperience(player, requestedLevel);
        }

        openFor(player, pos);
    }

    public static void openFor(
            ServerPlayer player,
            BlockPos lecternPos
    ) {
        ServerPlayNetworking.send(
                player,
                new OpenRadiantProgressionPayload(
                        lecternPos,
                        RadiantProgression.getOrderNetworkId(player),
                        RadiantProgression.getLevel(player),
                        player.experienceLevel
                )
        );
    }

    private static boolean canUseLectern(
            ServerPlayer player,
            BlockPos pos
    ) {
        return player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= MAX_INTERACTION_DISTANCE_SQUARED
                && hasWordsOfRadiance(player.level(), pos);
    }

    private static void sendInsufficientExperience(
            ServerPlayer player,
            int level
    ) {
        player.sendOverlayMessage(
                Component.translatable(
                        "message.stormlight.radiant.not_enough_xp",
                        RadiantProgression.experienceCost(level),
                        level
                )
        );
    }

    private static boolean hasWordsOfRadiance(
            Level level,
            BlockPos pos
    ) {
        if (!level.getBlockState(pos).is(Blocks.LECTERN)) {
            return false;
        }

        return level.getBlockEntity(pos)
                instanceof LecternBlockEntity lectern
                && lectern.getBook().is(ModItems.WORDS_OF_RADIANCE);
    }
}

package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UnlockRadiantLevelPayload(
        BlockPos lecternPos,
        int requestedLevel
) implements CustomPacketPayload {
    public static final Type<UnlockRadiantLevelPayload> TYPE =
            new Type<>(Stormlight.id("unlock_radiant_level"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            UnlockRadiantLevelPayload
            > CODEC = StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    UnlockRadiantLevelPayload::lecternPos,
                    ByteBufCodecs.VAR_INT,
                    UnlockRadiantLevelPayload::requestedLevel,
                    UnlockRadiantLevelPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

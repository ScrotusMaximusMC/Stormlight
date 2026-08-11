package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenRadiantProgressionPayload(
        BlockPos lecternPos,
        int orderNetworkId,
        int level
) implements CustomPacketPayload {
    public static final Type<OpenRadiantProgressionPayload> TYPE =
            new Type<>(Stormlight.id("open_radiant_progression"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenRadiantProgressionPayload
            > CODEC = StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenRadiantProgressionPayload::lecternPos,
                    ByteBufCodecs.VAR_INT,
                    OpenRadiantProgressionPayload::orderNetworkId,
                    ByteBufCodecs.VAR_INT,
                    OpenRadiantProgressionPayload::level,
                    OpenRadiantProgressionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

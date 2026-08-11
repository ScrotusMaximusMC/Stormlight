package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChooseRadiantOrderPayload(
        BlockPos lecternPos,
        int orderNetworkId
) implements CustomPacketPayload {
    public static final Type<ChooseRadiantOrderPayload> TYPE =
            new Type<>(Stormlight.id("choose_radiant_order"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChooseRadiantOrderPayload
            > CODEC = StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ChooseRadiantOrderPayload::lecternPos,
                    ByteBufCodecs.VAR_INT,
                    ChooseRadiantOrderPayload::orderNetworkId,
                    ChooseRadiantOrderPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

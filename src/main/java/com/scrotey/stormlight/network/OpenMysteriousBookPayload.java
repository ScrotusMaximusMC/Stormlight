package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenMysteriousBookPayload(
        BlockPos lecternPos
) implements CustomPacketPayload {

    public static final Type<OpenMysteriousBookPayload> TYPE =
            new Type<>(Stormlight.id("open_mysterious_book"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenMysteriousBookPayload
            > CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            OpenMysteriousBookPayload::lecternPos,
            OpenMysteriousBookPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
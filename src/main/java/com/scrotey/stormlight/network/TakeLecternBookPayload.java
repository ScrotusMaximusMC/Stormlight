package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TakeLecternBookPayload(
        BlockPos lecternPos
) implements CustomPacketPayload {

    public static final Type<TakeLecternBookPayload> TYPE =
            new Type<>(Stormlight.id("take_lectern_book"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TakeLecternBookPayload
            > CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TakeLecternBookPayload::lecternPos,
            TakeLecternBookPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
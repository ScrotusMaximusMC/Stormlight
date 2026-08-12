package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ToggleLashingPayload()
        implements CustomPacketPayload {

    public static final ToggleLashingPayload INSTANCE =
            new ToggleLashingPayload();

    public static final Type<ToggleLashingPayload> TYPE =
            new Type<>(Stormlight.id("toggle_lashing"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ToggleLashingPayload
            > CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

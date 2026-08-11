package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BreatheStormlightPayload()
        implements CustomPacketPayload {

    public static final BreatheStormlightPayload INSTANCE =
            new BreatheStormlightPayload();

    public static final Type<BreatheStormlightPayload> TYPE =
            new Type<>(Stormlight.id("breathe_stormlight"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BreatheStormlightPayload
            > CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

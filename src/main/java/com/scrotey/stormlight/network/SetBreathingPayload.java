package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetBreathingPayload(boolean breathing)
        implements CustomPacketPayload {

    public static final Type<SetBreathingPayload> TYPE =
            new Type<>(Stormlight.id("set_breathing"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetBreathingPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SetBreathingPayload::breathing,
                    SetBreathingPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StormlightStatusPayload(
        int charge,
        int capacity,
        boolean breathing
) implements CustomPacketPayload {

    public static final Type<StormlightStatusPayload> TYPE =
            new Type<>(Stormlight.id("stormlight_status"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StormlightStatusPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    StormlightStatusPayload::charge,
                    ByteBufCodecs.VAR_INT,
                    StormlightStatusPayload::capacity,
                    ByteBufCodecs.BOOL,
                    StormlightStatusPayload::breathing,
                    StormlightStatusPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

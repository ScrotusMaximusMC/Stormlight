package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ExhaleStormlightPayload()
        implements CustomPacketPayload {

    public static final ExhaleStormlightPayload INSTANCE =
            new ExhaleStormlightPayload();

    public static final Type<ExhaleStormlightPayload> TYPE =
            new Type<>(Stormlight.id("exhale_stormlight"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ExhaleStormlightPayload
            > CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

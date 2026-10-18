package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ToggleSprenSeekPayload() implements CustomPacketPayload {
    public static final ToggleSprenSeekPayload INSTANCE =
            new ToggleSprenSeekPayload();
    public static final Type<ToggleSprenSeekPayload> TYPE =
            new Type<>(Stormlight.id("toggle_spren_seek"));
    public static final StreamCodec<RegistryFriendlyByteBuf,
            ToggleSprenSeekPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

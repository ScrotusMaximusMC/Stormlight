package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent only to the protected player when their spren absorbs a hit. */
public record SprenShieldBlockPayload() implements CustomPacketPayload {
    public static final SprenShieldBlockPayload INSTANCE =
            new SprenShieldBlockPayload();
    public static final Type<SprenShieldBlockPayload> TYPE =
            new Type<>(Stormlight.id("spren_shield_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf,
            SprenShieldBlockPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

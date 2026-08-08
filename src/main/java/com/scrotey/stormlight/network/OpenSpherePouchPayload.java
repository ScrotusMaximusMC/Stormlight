package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenSpherePouchPayload()
        implements CustomPacketPayload {

    public static final OpenSpherePouchPayload INSTANCE =
            new OpenSpherePouchPayload();

    public static final Type<OpenSpherePouchPayload> TYPE =
            new Type<>(
                    Stormlight.id("open_sphere_pouch")
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenSpherePouchPayload
            > CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
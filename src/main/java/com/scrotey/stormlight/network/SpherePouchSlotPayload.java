package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SpherePouchSlotPayload()
        implements CustomPacketPayload {

    public static final SpherePouchSlotPayload INSTANCE =
            new SpherePouchSlotPayload();

    public static final Type<SpherePouchSlotPayload> TYPE =
            new Type<>(
                    Stormlight.id("sphere_pouch_slot")
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SpherePouchSlotPayload
            > CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
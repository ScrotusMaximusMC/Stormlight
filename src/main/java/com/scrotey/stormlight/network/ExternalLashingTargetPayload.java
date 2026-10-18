package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ExternalLashingTargetPayload(int entityId)
        implements CustomPacketPayload {

    public static final int NO_TARGET = -1;

    public static final Type<ExternalLashingTargetPayload> TYPE =
            new Type<>(Stormlight.id("external_lashing_target"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ExternalLashingTargetPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ExternalLashingTargetPayload::entityId,
                    ExternalLashingTargetPayload::new
            );

    public static ExternalLashingTargetPayload cleared() {
        return new ExternalLashingTargetPayload(NO_TARGET);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

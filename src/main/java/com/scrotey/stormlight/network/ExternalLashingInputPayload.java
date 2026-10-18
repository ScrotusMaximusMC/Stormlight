package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ExternalLashingInputPayload(boolean cancel)
        implements CustomPacketPayload {

    public static final Type<ExternalLashingInputPayload> TYPE =
            new Type<>(Stormlight.id("external_lashing_input"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ExternalLashingInputPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ExternalLashingInputPayload::cancel,
                    ExternalLashingInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

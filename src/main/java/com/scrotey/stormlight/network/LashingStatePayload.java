package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LashingStatePayload(
        boolean active,
        float directionX,
        float directionZ
) implements CustomPacketPayload {
    public static final Type<LashingStatePayload> TYPE =
            new Type<>(Stormlight.id("lashing_state"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            LashingStatePayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    LashingStatePayload::active,
                    ByteBufCodecs.FLOAT,
                    LashingStatePayload::directionX,
                    ByteBufCodecs.FLOAT,
                    LashingStatePayload::directionZ,
                    LashingStatePayload::new
            );

    public static LashingStatePayload inactive() {
        return new LashingStatePayload(false, 0.0F, 0.0F);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.lashing.LashingDirection;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LashingStatePayload(
        boolean active,
        boolean stabilising,
        float directionX,
        float directionY,
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
                    ByteBufCodecs.BOOL,
                    LashingStatePayload::stabilising,
                    ByteBufCodecs.FLOAT,
                    LashingStatePayload::directionX,
                    ByteBufCodecs.FLOAT,
                    LashingStatePayload::directionY,
                    ByteBufCodecs.FLOAT,
                    LashingStatePayload::directionZ,
                    LashingStatePayload::new
            );

    public static LashingStatePayload inactive() {
        return new LashingStatePayload(
                false,
                false,
                0.0F,
                0.0F,
                0.0F
        );
    }

    public static LashingStatePayload stabilisingWithoutLashing() {
        return new LashingStatePayload(
                false,
                true,
                0.0F,
                0.0F,
                0.0F
        );
    }

    public static LashingStatePayload active(
            LashingDirection direction,
            boolean stabilising
    ) {
        return new LashingStatePayload(
                true,
                stabilising,
                (float) direction.x(),
                (float) direction.y(),
                (float) direction.z()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

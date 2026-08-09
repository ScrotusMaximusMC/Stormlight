package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HighstormVisualPayload(
        int phaseOrdinal,
        int ticksRemaining
) implements CustomPacketPayload {

    public static final Type<HighstormVisualPayload> TYPE =
            new Type<>(
                    Stormlight.id("highstorm_visual")
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            HighstormVisualPayload
            > CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            HighstormVisualPayload::phaseOrdinal,
            ByteBufCodecs.VAR_INT,
            HighstormVisualPayload::ticksRemaining,
            HighstormVisualPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
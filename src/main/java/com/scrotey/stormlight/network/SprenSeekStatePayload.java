package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Phase 0 clears, 1 follows the spren, and 2 highlights the ore. */
public record SprenSeekStatePayload(
        int phase,
        int sprenId,
        BlockPos ore
) implements CustomPacketPayload {
    public static final Type<SprenSeekStatePayload> TYPE =
            new Type<>(Stormlight.id("spren_seek_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,
            SprenSeekStatePayload> CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SprenSeekStatePayload::phase,
                    ByteBufCodecs.VAR_INT,
                    SprenSeekStatePayload::sprenId,
                    BlockPos.STREAM_CODEC,
                    SprenSeekStatePayload::ore,
                    SprenSeekStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

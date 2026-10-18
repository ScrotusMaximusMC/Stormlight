package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShardbladeStatePayload(boolean summoned, boolean selected,
        boolean ready) implements CustomPacketPayload {
    public static final Type<ShardbladeStatePayload> TYPE =
            new Type<>(Stormlight.id("shardblade_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,
            ShardbladeStatePayload> CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL, ShardbladeStatePayload::summoned,
                    ByteBufCodecs.BOOL, ShardbladeStatePayload::selected,
                    ByteBufCodecs.BOOL, ShardbladeStatePayload::ready,
                    ShardbladeStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

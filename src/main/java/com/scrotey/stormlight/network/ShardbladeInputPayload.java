package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 0 summons and selects the blade; 2 dismisses it. */
public record ShardbladeInputPayload(int action) implements CustomPacketPayload {
    public static final Type<ShardbladeInputPayload> TYPE =
            new Type<>(Stormlight.id("shardblade_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,
            ShardbladeInputPayload> CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ShardbladeInputPayload::action,
                    ShardbladeInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Snapshot of the reader's progress, sent when they open the lectern book. */
public record OpenWayOfKingsPayload(BlockPos lecternPos, int orderNetworkId,
                                    int radiantLevel, int experienceLevel, int completedCuts)
        implements CustomPacketPayload {
    public static final Type<OpenWayOfKingsPayload> TYPE = new Type<>(Stormlight.id("open_way_of_kings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWayOfKingsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenWayOfKingsPayload::lecternPos,
            ByteBufCodecs.VAR_INT, OpenWayOfKingsPayload::orderNetworkId,
            ByteBufCodecs.VAR_INT, OpenWayOfKingsPayload::radiantLevel,
            ByteBufCodecs.VAR_INT, OpenWayOfKingsPayload::experienceLevel,
            ByteBufCodecs.VAR_INT, OpenWayOfKingsPayload::completedCuts,
            OpenWayOfKingsPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

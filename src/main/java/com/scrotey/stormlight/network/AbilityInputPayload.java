package com.scrotey.stormlight.network;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.breathing.AbilityId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AbilityInputPayload(AbilityId ability, Action action)
        implements CustomPacketPayload {

    public enum Action {
        TOGGLE,
        START,
        STOP
    }

    public static final Type<AbilityInputPayload> TYPE =
            new Type<>(Stormlight.id("ability_input"));

    private static final StreamCodec<RegistryFriendlyByteBuf, AbilityId> ABILITY_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(
                    ordinal -> AbilityId.values()[ordinal],
                    Enum::ordinal
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, Action> ACTION_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(
                    ordinal -> Action.values()[ordinal],
                    Enum::ordinal
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AbilityInputPayload
            > CODEC = StreamCodec.composite(
                    ABILITY_CODEC,
                    AbilityInputPayload::ability,
                    ACTION_CODEC,
                    AbilityInputPayload::action,
                    AbilityInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.scrotey.stormlight.highstorm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.scrotey.stormlight.Stormlight;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class HighstormSavedData extends SavedData {
    private static final Codec<HighstormPhase> PHASE_CODEC =
            Codec.STRING.xmap(
                    HighstormPhase::valueOf,
                    HighstormPhase::name
            );

    private static final Codec<HighstormSavedData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            PHASE_CODEC.fieldOf("phase")
                                    .forGetter(data -> data.phase),
                            Codec.LONG.fieldOf("ticks_remaining")
                                    .forGetter(data -> data.ticksRemaining),
                            Codec.BOOL.optionalFieldOf("initialized", false)
                                    .forGetter(data -> data.initialized)
                    ).apply(instance, HighstormSavedData::new)
            );

    private static final SavedDataType<HighstormSavedData> TYPE =
            new SavedDataType<>(
                    Stormlight.id("highstorm"),
                    HighstormSavedData::new,
                    CODEC,
                    null
            );

    private HighstormPhase phase;
    private long ticksRemaining;
    private boolean initialized;

    public HighstormSavedData() {
        this(HighstormPhase.CALM, 0, false);
    }

    private HighstormSavedData(
            HighstormPhase phase,
            long ticksRemaining,
            boolean initialized
    ) {
        this.phase = phase;
        this.ticksRemaining = Math.max(0, ticksRemaining);
        this.initialized = initialized;
    }

    public static HighstormSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public HighstormPhase getPhase() {
        return phase;
    }

    public long getTicksRemaining() {
        return ticksRemaining;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setPhase(
            HighstormPhase phase,
            long ticksRemaining
    ) {
        this.phase = phase;
        this.ticksRemaining = Math.max(0, ticksRemaining);
        this.initialized = true;
        setDirty();
    }

    public void tickDown() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
            setDirty();
        }
    }
}

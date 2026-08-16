package com.scrotey.stormlight.worldgen.chrysalis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.scrotey.stormlight.Stormlight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public final class ChrysalisSavedData extends SavedData {
    private static final int MAX_RECENT_SPAWNS = 20;

    private static final Codec<ChrysalisSavedData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            BlockPos.CODEC.listOf()
                                    .optionalFieldOf("recent_spawns", List.of())
                                    .forGetter(data -> data.recentSpawns),
                            BlockPos.CODEC.listOf()
                                    .optionalFieldOf("active_gemhearts", List.of())
                                    .forGetter(data -> data.activeGemhearts)
                    ).apply(instance, ChrysalisSavedData::new)
            );

    private static final SavedDataType<ChrysalisSavedData> TYPE =
            new SavedDataType<>(
                    Stormlight.id("chrysalis_spawns"),
                    ChrysalisSavedData::new,
                    CODEC,
                    null
            );

    private final List<BlockPos> recentSpawns;
    private final List<BlockPos> activeGemhearts;

    public ChrysalisSavedData() {
        this(List.of(), List.of());
    }

    private ChrysalisSavedData(
            List<BlockPos> recentSpawns,
            List<BlockPos> activeGemhearts
    ) {
        this.recentSpawns = new ArrayList<>(recentSpawns);
        this.activeGemhearts = new ArrayList<>(activeGemhearts);

        while (this.recentSpawns.size() > MAX_RECENT_SPAWNS) {
            this.recentSpawns.remove(0);
        }
    }

    public static ChrysalisSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public int getActiveGemheartCount() {
        return activeGemhearts.size();
    }

    public BlockPos findNearestActiveGemheart(BlockPos origin) {
        BlockPos nearest = null;
        long nearestDistanceSquared = Long.MAX_VALUE;

        for (BlockPos pos : activeGemhearts) {
            long distanceSquared = horizontalDistanceSquared(origin, pos);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = pos;
            }
        }

        return nearest;
    }

    public boolean isNearActiveGemheart(
            BlockPos candidate,
            int radius
    ) {
        long radiusSquared = (long) radius * radius;

        for (BlockPos pos : activeGemhearts) {
            if (horizontalDistanceSquared(candidate, pos) <= radiusSquared) {
                return true;
            }
        }

        return false;
    }

    public boolean isNearRecentSpawn(
            BlockPos candidate,
            int radius
    ) {
        long radiusSquared = (long) radius * radius;

        for (BlockPos pos : recentSpawns) {
            if (horizontalDistanceSquared(candidate, pos) <= radiusSquared) {
                return true;
            }
        }

        return false;
    }

    public void recordSpawn(
            BlockPos chrysalisPos,
            BlockPos gemheartPos
    ) {
        recentSpawns.add(chrysalisPos.immutable());
        while (recentSpawns.size() > MAX_RECENT_SPAWNS) {
            recentSpawns.remove(0);
        }

        activeGemhearts.add(gemheartPos.immutable());
        setDirty();
    }

    public void markGemheartMined(BlockPos pos) {
        boolean removed = activeGemhearts.removeIf(
                tracked -> tracked.equals(pos)
        );

        if (removed) {
            setDirty();
        }
    }

    /**
     * Defensive cleanup in case a Gemheart was removed by a command or some
     * other non-player action. Unloaded positions are kept; a player cannot
     * mine a Gemheart in an unloaded chunk anyway.
     */
    public void pruneMissingGemhearts(ServerLevel level) {
        boolean removed = activeGemhearts.removeIf(pos ->
                !level.isInWorldBounds(pos)
                        || (
                        level.hasChunkAt(pos)
                                && !ChrysalisSpawner.isGemheartBlock(
                                        level.getBlockState(pos).getBlock()
                                )
                )
        );

        if (removed) {
            setDirty();
        }
    }

    private static long horizontalDistanceSquared(
            BlockPos a,
            BlockPos b
    ) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}

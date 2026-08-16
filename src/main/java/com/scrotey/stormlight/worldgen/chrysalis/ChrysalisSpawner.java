package com.scrotey.stormlight.worldgen.chrysalis;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.worldgen.feature.ChrysalisFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handles event-driven chrysalis spawning without blocking the server tick.
 *
 * A search begins when the Highstorm proper starts. Long-range biome probing
 * is cheap and does not generate chunks. When a promising Badlands site is
 * found, only one chunk of its validation area is prepared at a time. A valid
 * site may therefore be cached several minutes before the storm actually
 * finishes.
 *
 * The eligibility cap and 25% spawn roll still happen only after Passing ends.
 */
public final class ChrysalisSpawner {
    private static final float SPAWN_CHANCE = 1.0F;
    private static final int MAX_UNMINED_BEFORE_BLOCKING = 8;
    private static final int ACTIVE_GEMHEART_EXCLUSION_RADIUS = 500;
    private static final int RECENT_SPAWN_EXCLUSION_RADIUS = 300;

    private static final int MIN_SEARCH_RADIUS = 128;
    private static final int MAX_SEARCH_RADIUS = 6_000;
    private static final int SEARCH_RING_STEP = 128;
    private static final int TARGET_ARC_SPACING = 192;

    /* Cheap noise-only biome probes allowed in a single server tick. */
    private static final int NOISE_PROBES_PER_TICK = 48;

    /*
     * Preparing a genuinely new chunk can be expensive. Do at most one every
     * half-second, allowing the several-minute Highstorm to absorb the work.
     */
    private static final int CHUNK_PREPARE_INTERVAL_TICKS = 10;

    /* Never let one Highstorm search generate terrain indefinitely. */
    private static final int MAX_TERRAIN_CANDIDATES = 60;

    private static final int[][] LOCAL_OFFSETS = {
            {0, 0},
            {32, 0}, {-32, 0}, {0, 32}, {0, -32},
            {32, 32}, {32, -32}, {-32, 32}, {-32, -32},
            {64, 0}, {-64, 0}, {0, 64}, {0, -64},
            {64, 64}, {64, -64}, {-64, 64}, {-64, -64},
            {96, 0}, {-96, 0}, {0, 96}, {0, -96}
    };

    private static PendingSearch pendingSearch;

    private ChrysalisSpawner() {
    }

    /**
     * Starts (or restarts) the quiet pre-search for a new Highstorm.
     */
    public static void beginHighstormSearch(MinecraftServer server) {
        cancelSearch();

        ServerLevel level = server.overworld();
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        ChrysalisSavedData data = ChrysalisSavedData.get(server);
        data.pruneMissingGemhearts(level);

        List<BlockPos> origins = new ArrayList<>();
        for (ServerPlayer player : players) {
            origins.add(player.blockPosition().immutable());
        }

        pendingSearch = new PendingSearch(origins);

        Stormlight.LOGGER.debug(
                "Started incremental chrysalis site search for {} player origin(s).",
                origins.size()
        );
    }

    /**
     * Called every server tick by HighstormManager. All expensive work is
     * deliberately budgeted across ticks.
     */
    public static void tick(MinecraftServer server) {
        PendingSearch search = pendingSearch;
        if (search == null) {
            return;
        }

        /* A pre-found site simply waits for Passing to finish. */
        if (!search.spawnAuthorized && search.cachedCandidate != null) {
            return;
        }

        if (search.exhausted && search.activeValidation == null) {
            if (search.spawnAuthorized) {
                Stormlight.LOGGER.info(
                        "Chrysalis spawn roll succeeded, but the incremental search exhausted its limits without finding a suitable Badlands site."
                );
                cancelSearch();
            }
            return;
        }

        ServerLevel level = server.overworld();
        ChrysalisSavedData data = ChrysalisSavedData.get(server);

        /*
         * If there are already too many unmined Gemhearts, this Highstorm
         * cannot produce another chrysalis. Keep the pending search alive but
         * do absolutely no search/chunk work until the active count drops back
         * to 8 or fewer. If a Gemheart is mined during the storm, the search
         * simply resumes on a later tick from where it paused.
         */
        if (data.getActiveGemheartCount() > MAX_UNMINED_BEFORE_BLOCKING) {
            return;
        }

        if (search.activeValidation != null) {
            tickTerrainValidation(server, level, data, search);
            return;
        }

        /*
         * After the roll succeeds, a cached site only needs its chunks
         * prepared for placement. Its validated surface Y and rotation are
         * already authoritative and must never be recalculated.
         */
        if (search.spawnAuthorized && search.cachedCandidate != null) {
            ChrysalisFeature.ValidatedSite cached = search.cachedCandidate;
            search.cachedCandidate = null;
            search.activeValidation = new TerrainValidation(cached);
            return;
        }

        int probes = 0;
        while (probes < NOISE_PROBES_PER_TICK && !search.exhausted) {
            BlockPos anchor = search.nextNoiseProbe();
            if (anchor == null) {
                search.exhausted = true;
                break;
            }

            probes++;
            search.noiseProbesAttempted++;

            if (!isBadlandsAt(level, anchor.getX(), anchor.getZ())) {
                continue;
            }

            BlockPos candidate = findCheapLocalCandidate(
                    level,
                    anchor,
                    data,
                    search
            );

            if (candidate == null) {
                continue;
            }

            search.terrainCandidatesAttempted++;
            if (search.terrainCandidatesAttempted > MAX_TERRAIN_CANDIDATES) {
                search.exhausted = true;
                break;
            }

            search.activeValidation = new TerrainValidation(candidate);
            break;
        }
    }

    /**
     * Passing has ended. Only now do the cap check and the 25% roll.
     */
    public static void onHighstormFinished(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ChrysalisSavedData data = ChrysalisSavedData.get(server);
        data.pruneMissingGemhearts(level);

        if (data.getActiveGemheartCount() > MAX_UNMINED_BEFORE_BLOCKING) {
            cancelSearch();
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextFloat() >= SPAWN_CHANCE) {
            cancelSearch();
            return;
        }

        /*
         * A forced /highstorm passing can reach here without ever having had a
         * HIGHSTORM phase. Start a search now rather than doing synchronous
         * work; it will continue quietly until a site is found or exhausted.
         */
        if (pendingSearch == null) {
            beginHighstormSearch(server);
        }

        if (pendingSearch == null) {
            return;
        }

        pendingSearch.spawnAuthorized = true;

        server.getPlayerList().broadcastSystemMessage(
                Component.literal(
                        "DEV: Chrysalis spawn authorised. Waiting for a suitable site."
                ).withStyle(ChatFormatting.LIGHT_PURPLE),
                false
        );

        /* If a site was cached, tick() will revalidate and place it shortly. */
        if (pendingSearch.exhausted
                && pendingSearch.cachedCandidate == null
                && pendingSearch.activeValidation == null) {
            Stormlight.LOGGER.info(
                    "Chrysalis spawn roll succeeded, but no suitable Badlands site was found during the Highstorm search."
            );
            cancelSearch();
        }
    }

    /**
     * Human-readable developer snapshot used by /chrysalis status.
     */
    public static String getDebugStatus(MinecraftServer server) {
        ChrysalisSavedData data = ChrysalisSavedData.get(server);
        int activeGemhearts = data.getActiveGemheartCount();

        PendingSearch search = pendingSearch;
        if (search == null) {
            return "Active Gemhearts: " + activeGemhearts
                    + " | Search: none";
        }

        boolean pausedByGemheartCap =
                activeGemhearts > MAX_UNMINED_BEFORE_BLOCKING;

        String cachedSite = search.cachedCandidate == null
                ? "none"
                : formatPos(search.cachedCandidate.surfaceOrigin());

        String validation = "none";
        if (search.activeValidation != null) {
            TerrainValidation active = search.activeValidation;
            validation = formatPos(active.candidate)
                    + " chunks "
                    + Math.min(
                            active.nextChunkIndex,
                            active.chunkCoordinates.length
                    )
                    + "/"
                    + active.chunkCoordinates.length;
        }

        return "Active Gemhearts: " + activeGemhearts
                + " | Search: "
                + (search.exhausted ? "exhausted" : "running")
                + " | Paused by cap: " + pausedByGemheartCap
                + " | Spawn authorised: " + search.spawnAuthorized
                + " | Noise probes: " + search.noiseProbesAttempted
                + " | Terrain candidates: "
                + search.terrainCandidatesAttempted
                + "/"
                + MAX_TERRAIN_CANDIDATES
                + " | Cached site: " + cachedSite
                + " | Validating: " + validation;
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX()
                + ", " + pos.getY()
                + ", " + pos.getZ()
                + "]";
    }

    /** Cancel pending work when a Highstorm is forcibly reset/stopped. */
    public static void cancelSearch() {
        pendingSearch = null;
    }

    private static void tickTerrainValidation(
            MinecraftServer server,
            ServerLevel level,
            ChrysalisSavedData data,
            PendingSearch search
    ) {
        TerrainValidation validation = search.activeValidation;
        if (validation == null) {
            return;
        }

        if (validation.ticksUntilNextChunk > 0) {
            validation.ticksUntilNextChunk--;
            return;
        }

        /*
         * Prepare exactly one required chunk at a time. For a brand-new
         * candidate, capture every surface column in that chunk immediately
         * while the chunk is definitely loaded.
         */
        if (validation.nextChunkIndex < validation.chunkCoordinates.length) {
            int[] chunk = validation.chunkCoordinates[
                    validation.nextChunkIndex++
            ];

            level.getChunk(chunk[0], chunk[1]);

            if (validation.prevalidatedSite == null) {
                captureChunkSurface(
                        level,
                        chunk[0],
                        chunk[1],
                        validation.surfaceSamples
                );
            }

            validation.ticksUntilNextChunk =
                    CHUNK_PREPARE_INTERVAL_TICKS;
            return;
        }

        search.activeValidation = null;

        ChrysalisFeature.ValidatedSite validatedSite;

        if (validation.prevalidatedSite != null) {
            validatedSite = validation.prevalidatedSite;
        } else {
            BlockPos candidate = validation.candidate;

            /* Re-check cheap dynamic exclusions before accepting the site. */
            if (!isCandidateAllowed(level, candidate, data)) {
                return;
            }

            Optional<ChrysalisFeature.ValidatedSite> validated =
                    ChrysalisFeature.validateCapturedSurface(
                            level,
                            candidate,
                            validation.surfaceSamples
                    );

            if (validated.isEmpty()) {
                return;
            }

            validatedSite = validated.get();
        }

        if (!search.spawnAuthorized) {
            search.cachedCandidate = validatedSite;
            Stormlight.LOGGER.debug(
                    "Cached suitable chrysalis site at {} while Highstorm is still in progress.",
                    validatedSite.surfaceOrigin()
            );
            return;
        }

        /*
         * Re-check only the saved-data / biome exclusions here. The surface Y
         * and flatness were already captured from loaded terrain and are not
         * recalculated.
         */
        if (!isCandidateAllowed(
                level,
                validatedSite.surfaceOrigin(),
                data
        )) {
            return;
        }

        Optional<ChrysalisFeature.PlacementResult> placed =
                ChrysalisFeature.tryPlaceAtValidatedSite(
                        level,
                        validatedSite,
                        level.getRandom()
                );

        if (placed.isEmpty()) {
            return;
        }

        ChrysalisFeature.PlacementResult result = placed.get();
        data.recordSpawn(
                result.chrysalisPos(),
                result.gemheartPos()
        );

        server.getPlayerList().broadcastSystemMessage(
                Component.literal(
                        "A chrysalis has spawned in the Shattered Plains."
                ).withStyle(ChatFormatting.GOLD),
                false
        );

        cancelSearch();
    }

    /**
     * Captures the surface data for every column in one freshly prepared
     * chunk. This is the critical part of the distant-search fix: no future
     * step needs to ask an unloaded chunk for its height.
     */
    private static void captureChunkSurface(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            Map<Long, ChrysalisFeature.SurfaceSample> destination
    ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;

                int surfaceAirY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        worldX,
                        worldZ
                );

                int surfaceY = surfaceAirY - 1;
                BlockPos surfacePos = new BlockPos(
                        worldX,
                        surfaceY,
                        worldZ
                );

                boolean inBounds = level.isInWorldBounds(surfacePos);
                boolean dry = inBounds
                        && level.getFluidState(surfacePos).isEmpty();
                boolean badlands = inBounds
                        && isBadlandsBiome(level.getBiome(surfacePos));

                destination.put(
                        horizontalKey(worldX, worldZ),
                        new ChrysalisFeature.SurfaceSample(
                                surfaceAirY,
                                dry,
                                badlands
                        )
                );
            }
        }
    }

    /**
     * Finds a local site worth spending chunk-generation time on. Everything
     * in this method is cheap/noise/saved-data only.
     */
    private static BlockPos findCheapLocalCandidate(
            ServerLevel level,
            BlockPos anchor,
            ChrysalisSavedData data,
            PendingSearch search
    ) {
        for (int[] offset : LOCAL_OFFSETS) {
            BlockPos candidate = new BlockPos(
                    anchor.getX() + offset[0],
                    anchor.getY(),
                    anchor.getZ() + offset[1]
            );

            long key = horizontalKey(candidate);
            if (!search.testedCandidates.add(key)) {
                continue;
            }

            if (!isBadlandsAt(level, candidate.getX(), candidate.getZ())) {
                continue;
            }

            if (data.isNearActiveGemheart(
                    candidate,
                    ACTIVE_GEMHEART_EXCLUSION_RADIUS
            )) {
                continue;
            }

            if (data.isNearRecentSpawn(
                    candidate,
                    RECENT_SPAWN_EXCLUSION_RADIUS
            )) {
                continue;
            }

            return candidate;
        }

        return null;
    }

    private static boolean isCandidateAllowed(
            ServerLevel level,
            BlockPos candidate,
            ChrysalisSavedData data
    ) {
        return isBadlandsAt(level, candidate.getX(), candidate.getZ())
                && !data.isNearActiveGemheart(
                        candidate,
                        ACTIVE_GEMHEART_EXCLUSION_RADIUS
                )
                && !data.isNearRecentSpawn(
                        candidate,
                        RECENT_SPAWN_EXCLUSION_RADIUS
                );
    }

    private static boolean isBadlandsBiome(Holder<Biome> biome) {
        return biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS)
                || biome.is(Biomes.WOODED_BADLANDS);
    }

    private static boolean isBadlandsAt(
            ServerLevel level,
            int blockX,
            int blockZ
    ) {
        Holder<Biome> biome = level.getUncachedNoiseBiome(
                QuartPos.fromBlock(blockX),
                QuartPos.fromBlock(64),
                QuartPos.fromBlock(blockZ)
        );

        return isBadlandsBiome(biome);
    }

    public static boolean isGemheartBlock(Block block) {
        return block == ModBlocks.DIAMOND_GEMHEART
                || block == ModBlocks.GARNET_GEMHEART
                || block == ModBlocks.RUBY_GEMHEART
                || block == ModBlocks.SAPPHIRE_GEMHEART
                || block == ModBlocks.EMERALD_GEMHEART;
    }

    private static long horizontalKey(BlockPos pos) {
        return horizontalKey(pos.getX(), pos.getZ());
    }

    private static long horizontalKey(int x, int z) {
        return BlockPos.asLong(x, 0, z);
    }

    private static final class PendingSearch {
        private final List<SearchCursor> cursors;
        private final Set<Long> testedCandidates = new HashSet<>();

        private int nextCursorIndex;
        private int noiseProbesAttempted;
        private int terrainCandidatesAttempted;
        private boolean spawnAuthorized;
        private boolean exhausted;
        private ChrysalisFeature.ValidatedSite cachedCandidate;
        private TerrainValidation activeValidation;

        private PendingSearch(List<BlockPos> origins) {
            this.cursors = new ArrayList<>();
            for (BlockPos origin : origins) {
                cursors.add(new SearchCursor(origin));
            }
        }

        private BlockPos nextNoiseProbe() {
            if (cursors.isEmpty()) {
                return null;
            }

            int checked = 0;
            while (checked < cursors.size()) {
                if (nextCursorIndex >= cursors.size()) {
                    nextCursorIndex = 0;
                }

                SearchCursor cursor = cursors.get(nextCursorIndex++);
                checked++;

                BlockPos result = cursor.next();
                if (result != null) {
                    return result;
                }
            }

            return null;
        }
    }

    /** Incremental concentric-ring biome search around one player's position. */
    private static final class SearchCursor {
        private final BlockPos origin;

        private boolean testedOrigin;
        private int radius = MIN_SEARCH_RADIUS;
        private int sampleIndex;
        private int samplesAtRadius = samplesForRadius(MIN_SEARCH_RADIUS);

        private SearchCursor(BlockPos origin) {
            this.origin = origin;
        }

        private BlockPos next() {
            if (!testedOrigin) {
                testedOrigin = true;
                return origin;
            }

            if (radius > MAX_SEARCH_RADIUS) {
                return null;
            }

            double angleOffset =
                    ((radius * 0.6180339887498949D) % 1.0D)
                            * Math.PI * 2.0D;

            double angle = angleOffset
                    + (Math.PI * 2.0D * sampleIndex / samplesAtRadius);

            int x = origin.getX()
                    + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ()
                    + (int) Math.round(Math.sin(angle) * radius);

            sampleIndex++;
            if (sampleIndex >= samplesAtRadius) {
                radius += SEARCH_RING_STEP;
                sampleIndex = 0;
                if (radius <= MAX_SEARCH_RADIUS) {
                    samplesAtRadius = samplesForRadius(radius);
                }
            }

            return new BlockPos(x, origin.getY(), z);
        }

        private static int samplesForRadius(int radius) {
            return Math.max(
                    16,
                    (int) Math.ceil(
                            (2.0D * Math.PI * radius)
                                    / TARGET_ARC_SPACING
                    )
            );
        }
    }

    /**
     * Loads the same conservative 3x3 chunk area as the old synchronous code,
     * but one chunk at a time instead of all nine in a single server tick.
     */
    private static final class TerrainValidation {
        private final BlockPos candidate;
        private final ChrysalisFeature.ValidatedSite prevalidatedSite;
        private final Map<Long, ChrysalisFeature.SurfaceSample> surfaceSamples =
                new HashMap<>();
        private final int[][] chunkCoordinates = new int[9][2];

        private int nextChunkIndex;
        private int ticksUntilNextChunk;

        private TerrainValidation(BlockPos candidate) {
            this(candidate, null);
        }

        private TerrainValidation(
                ChrysalisFeature.ValidatedSite prevalidatedSite
        ) {
            this(
                    prevalidatedSite.surfaceOrigin(),
                    prevalidatedSite
            );
        }

        private TerrainValidation(
                BlockPos candidate,
                ChrysalisFeature.ValidatedSite prevalidatedSite
        ) {
            this.candidate = candidate.immutable();
            this.prevalidatedSite = prevalidatedSite;

            int centreChunkX = candidate.getX() >> 4;
            int centreChunkZ = candidate.getZ() >> 4;

            int index = 0;
            for (int chunkX = centreChunkX - 1;
                 chunkX <= centreChunkX + 1;
                 chunkX++) {
                for (int chunkZ = centreChunkZ - 1;
                     chunkZ <= centreChunkZ + 1;
                     chunkZ++) {
                    chunkCoordinates[index][0] = chunkX;
                    chunkCoordinates[index][1] = chunkZ;
                    index++;
                }
            }
        }
    }

}

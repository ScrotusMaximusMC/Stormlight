package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.network.SprenSeekStatePayload;
import com.scrotey.stormlight.progression.SprenBondProgression;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SprenSeekManager {
    public static final int RADIUS = 75;
    private static final int SECTIONS_PER_TICK = 24;
    private static final int COOLDOWN_TICKS = 20 * 120;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private SprenSeekManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(SprenSeekManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> SESSIONS.remove(
                        handler.getPlayer().getUUID())
        );
    }

    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (SESSIONS.containsKey(id)) {
            cancel(player);
            return;
        }
        long remaining = COOLDOWN_UNTIL.getOrDefault(id, 0L)
                - player.level().getGameTime();
        if (remaining > 0) {
            player.sendOverlayMessage(Component.translatable(
                    "message.stormlight.spren_seek.cooldown",
                    (remaining + 19) / 20));
            return;
        }
        if (SprenBondProgression.getLevel(player) < 1) {
            message(player, "spren_seek.locked");
            return;
        }
        SprenEntity spren = SprenManager.getFor(player);
        if (spren == null) {
            message(player, "spren_seek.no_spren");
            return;
        }

        SESSIONS.put(id, new Session(player.level(),
                player.blockPosition(), spren.getUUID()));
        COOLDOWN_UNTIL.put(id,
                player.level().getGameTime() + COOLDOWN_TICKS);
        message(player, "spren_seek.searching");
    }

    public static void returned(ServerPlayer player, BlockPos ore) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.phase != Phase.RETURNING
                || !ore.equals(session.ore)) {
            return;
        }
        SprenEntity spren = SprenManager.getFor(player);
        if (spren == null) {
            cancel(player);
            return;
        }
        session.phase = Phase.TRAVELING;
        spren.startSeek(SprenSeekPathfinder.route(
                session.level, spren.position(),
                player.blockPosition(), ore), ore);
        send(player, 1, spren.getId(), ore);
        session.level.playSound(null, spren.blockPosition(),
                SoundEvents.ALLAY_AMBIENT_WITH_ITEM,
                SoundSource.AMBIENT, 0.8F, 1.35F);
    }

    public static void arrived(ServerPlayer player, BlockPos ore) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.phase != Phase.TRAVELING
                || !ore.equals(session.ore)) {
            return;
        }
        session.phase = Phase.HIGHLIGHTING;
        send(player, 2, session.sprenId, ore);
        message(player, "spren_seek.found");
        player.level().playSound(null, ore,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.AMBIENT, 0.8F, 1.5F);
    }

    public static void cancel(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            SprenEntity spren = SprenManager.getFor(player);
            if (spren != null) {
                spren.stopSeek();
            }
            send(player, 0, 0, BlockPos.ZERO);
            message(player, "spren_seek.cancelled");
        }
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> iterator =
                SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null || player.level() != session.level) {
                iterator.remove();
                continue;
            }
            SprenEntity spren = SprenManager.getFor(player);
            if (spren == null || !spren.getUUID().equals(session.sprenUuid)) {
                send(player, 0, 0, BlockPos.ZERO);
                iterator.remove();
                continue;
            }

            if (session.phase == Phase.SEARCHING) {
                if (scan(session)) {
                    session.reachability =
                            new SprenSeekPathfinder.ReachabilitySearch(
                                    session.level, session.origin,
                                    session.exposedOres);
                    session.phase = Phase.EVALUATING;
                }
            } else if (session.phase == Phase.EVALUATING) {
                session.reachability.advance(2_000);
                if (session.reachability.done()) {
                    BlockPos exposed = session.reachability.result();
                    if (exposed != null) session.ore = exposed;
                    session.reachability = null;
                    if (session.ore == null) {
                        message(player, "spren_seek.none_found");
                        iterator.remove();
                    } else {
                        session.phase = Phase.RETURNING;
                        session.sprenId = spren.getId();
                        spren.startSeekReturn(session.ore);
                        message(player, "spren_seek.returning");
                    }
                }
            } else if (!SprenSeekOres.isOre(
                    session.level.getBlockState(session.ore))) {
                spren.stopSeek();
                send(player, 0, 0, BlockPos.ZERO);
                iterator.remove();
            } else if (session.phase == Phase.TRAVELING
                    && session.level.getGameTime() % 65 == 0) {
                session.level.playSound(null,
                        spren.blockPosition(),
                        SoundEvents.ALLAY_AMBIENT_WITH_ITEM,
                        SoundSource.AMBIENT, 0.45F, 1.5F);
            }
        }
    }

    private static boolean scan(Session session) {
        for (int checked = 0; checked < SECTIONS_PER_TICK;
                checked++) {
            if (session.sectionX > session.maxSectionX) {
                return true;
            }
            int chunkX = session.sectionX;
            int sectionY = session.sectionY;
            int chunkZ = session.sectionZ;
            session.advance();

            LevelChunk chunk = session.level.getChunkSource()
                    .getChunkNow(chunkX, chunkZ);
            if (chunk == null) {
                continue;
            }
            int index = session.level.getSectionIndex(sectionY << 4);
            if (index < 0 || index >= chunk.getSections().length) {
                continue;
            }
            LevelChunkSection section = chunk.getSections()[index];
            if (!section.maybeHas(SprenSeekOres::isOre)) {
                continue;
            }
            scanSection(session, section, chunkX, sectionY, chunkZ);
        }
        return false;
    }

    private static void scanSection(Session session,
            LevelChunkSection section, int chunkX,
            int sectionY, int chunkZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 16; y++) {
            int worldY = (sectionY << 4) + y;
            int dy = worldY - session.origin.getY();
            if (Math.abs(dy) > RADIUS) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                int worldZ = (chunkZ << 4) + z;
                int dz = worldZ - session.origin.getZ();
                if (Math.abs(dz) > RADIUS) {
                    continue;
                }
                for (int x = 0; x < 16; x++) {
                    int worldX = (chunkX << 4) + x;
                    int dx = worldX - session.origin.getX();
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq > RADIUS * RADIUS
                            || !SprenSeekOres.isOre(
                                    section.getBlockState(x, y, z))) {
                        continue;
                    }
                    BlockPos ore = pos.set(worldX, worldY,
                            worldZ).immutable();
                    if (distanceSq < session.bestDistanceSq) {
                        session.bestDistanceSq = distanceSq;
                        session.ore = ore;
                    }
                    for (Direction direction : Direction.values()) {
                        BlockPos adjacent = ore.relative(direction);
                        if (session.level.hasChunkAt(adjacent)
                                && session.level.getBlockState(adjacent)
                                        .isAir()) {
                            session.exposedOres.add(ore);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void send(ServerPlayer player, int phase,
            int sprenId, BlockPos ore) {
        ServerPlayNetworking.send(player,
                new SprenSeekStatePayload(phase, sprenId, ore));
    }

    private static void message(ServerPlayer player, String suffix) {
        player.sendOverlayMessage(Component.translatable(
                "message.stormlight." + suffix));
    }

    private enum Phase {
        SEARCHING, EVALUATING, RETURNING, TRAVELING, HIGHLIGHTING
    }

    private static final class Session {
        private final ServerLevel level;
        private final BlockPos origin;
        private final UUID sprenUuid;
        private Phase phase = Phase.SEARCHING;
        private final int minSectionY;
        private final int minSectionZ;
        private final int maxSectionX;
        private final int maxSectionY;
        private final int maxSectionZ;
        private int sectionX;
        private int sectionY;
        private int sectionZ;
        private double bestDistanceSq = Double.POSITIVE_INFINITY;
        private final Set<BlockPos> exposedOres = new HashSet<>();
        private SprenSeekPathfinder.ReachabilitySearch reachability;
        private BlockPos ore;
        private int sprenId;

        private Session(ServerLevel level, BlockPos origin,
                UUID sprenUuid) {
            this.level = level;
            this.origin = origin;
            this.sprenUuid = sprenUuid;
            sectionX = Math.floorDiv(origin.getX() - RADIUS, 16);
            sectionY = Math.max(level.getMinSectionY(),
                    Math.floorDiv(origin.getY() - RADIUS, 16));
            sectionZ = Math.floorDiv(origin.getZ() - RADIUS, 16);
            minSectionY = sectionY;
            minSectionZ = sectionZ;
            maxSectionX = Math.floorDiv(origin.getX() + RADIUS, 16);
            maxSectionY = Math.min(level.getMaxSectionY(),
                    Math.floorDiv(origin.getY() + RADIUS, 16));
            maxSectionZ = Math.floorDiv(origin.getZ() + RADIUS, 16);
        }

        private void advance() {
            if (++sectionZ > maxSectionZ) {
                sectionZ = minSectionZ;
                if (++sectionY > maxSectionY) {
                    sectionY = minSectionY;
                    sectionX++;
                }
            }
        }
    }
}

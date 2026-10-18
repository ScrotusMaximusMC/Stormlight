package com.scrotey.stormlight.spren;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Finds a route through loaded air blocks, then a final hop into the ore. */
final class SprenSeekPathfinder {
    private static final int MAX_VISITED = 24_000;
    private static final int MAX_EXPOSED_VISITED = 60_000;
    private static final int MAX_DISTANCE_FROM_PLAYER = 80;

    private SprenSeekPathfinder() {
    }

    /** Finds ore beside air connected to the player's cave, in walking-distance order. */
    static BlockPos reachableExposedOre(ServerLevel level,
            BlockPos playerPos, Set<BlockPos> exposedOres) {
        ReachabilitySearch search = new ReachabilitySearch(
                level, playerPos, exposedOres);
        while (!search.done()) {
            search.advance(2_000);
        }
        return search.result();
    }

    static final class ReachabilitySearch {
        private final ServerLevel level;
        private final BlockPos origin;
        private final Set<BlockPos> exposedOres;
        private final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        private final Set<BlockPos> visited = new HashSet<>();
        private BlockPos result;

        ReachabilitySearch(ServerLevel level, BlockPos origin,
                Set<BlockPos> exposedOres) {
            this.level = level;
            this.origin = origin;
            this.exposedOres = exposedOres;
            BlockPos start = nearestAir(level, origin);
            if (start != null && !exposedOres.isEmpty()) {
                open.add(start);
                visited.add(start);
            }
        }

        void advance(int budget) {
            for (int checked = 0; checked < budget && !done(); checked++) {
                BlockPos current = open.removeFirst();
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (exposedOres.contains(next)) {
                        result = next;
                        return;
                    }
                    if (visited.contains(next)
                            || next.distSqr(origin)
                                    > SprenSeekManager.RADIUS
                                            * SprenSeekManager.RADIUS
                            || !level.hasChunkAt(next)
                            || !level.getBlockState(next).isAir()) {
                        continue;
                    }
                    visited.add(next);
                    open.addLast(next);
                }
            }
        }

        boolean done() {
            return result != null || open.isEmpty()
                    || visited.size() >= MAX_EXPOSED_VISITED;
        }

        BlockPos result() {
            return result;
        }
    }

    static List<Vec3> route(
            ServerLevel level,
            Vec3 sprenPosition,
            BlockPos playerPos,
            BlockPos ore
    ) {
        BlockPos start = nearestAir(level,
                BlockPos.containing(sprenPosition));
        if (start == null) {
            return List.of(Vec3.atCenterOf(ore));
        }

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::score)
        );
        Map<BlockPos, Integer> steps = new HashMap<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(new Node(start, distance(start, ore)));
        steps.put(start, 0);

        BlockPos closest = start;
        double closestDistance = distance(start, ore);

        while (!open.isEmpty() && visited.size() < MAX_VISITED) {
            Node node = open.poll();
            BlockPos current = node.pos();
            if (!visited.add(current)) {
                continue;
            }

            double remaining = distance(current, ore);
            if (remaining < closestDistance) {
                closest = current;
                closestDistance = remaining;
            }
            if (current.distManhattan(ore) == 1) {
                closest = current;
                break;
            }

            int nextSteps = steps.get(current) + 1;
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)
                        || next.distManhattan(playerPos)
                        > MAX_DISTANCE_FROM_PLAYER * 3
                        || !level.hasChunkAt(next)
                        || !level.getBlockState(next).isAir()
                        || nextSteps >= steps.getOrDefault(
                                next, Integer.MAX_VALUE)) {
                    continue;
                }

                steps.put(next, nextSteps);
                previous.put(next, current);
                open.add(new Node(next,
                        nextSteps + distance(next, ore)));
            }
        }

        List<Vec3> route = new ArrayList<>();
        route.add(Vec3.atCenterOf(start));
        for (BlockPos pos = closest;
                pos != null && !pos.equals(start);
                pos = previous.get(pos)) {
            route.add(Vec3.atCenterOf(pos));
        }
        java.util.Collections.reverse(route.subList(1, route.size()));
        route.add(Vec3.atCenterOf(ore));
        return route;
    }

    private static BlockPos nearestAir(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-2, -2, -2),
                origin.offset(2, 2, 2))) {
            if (!level.hasChunkAt(pos)
                    || !level.getBlockState(pos).isAir()) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }

    private static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    private record Node(BlockPos pos, double score) {
    }
}

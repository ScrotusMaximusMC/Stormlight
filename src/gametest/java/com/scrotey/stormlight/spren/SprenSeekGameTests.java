package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.block.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public class SprenSeekGameTests {
    @GameTest
    public void reachableCaveOreBeatsSealedOre(
            GameTestHelper helper) {
        BlockPos buried = new BlockPos(2, 2, 2);
        BlockPos exposed = new BlockPos(5, 2, 2);
        helper.setBlock(buried, ModBlocks.GARNET_ORE);
        helper.setBlock(exposed, Blocks.DIAMOND_ORE);
        for (Direction direction : Direction.values()) {
            helper.setBlock(buried.relative(direction), Blocks.STONE);
        }
        BlockPos selected = SprenSeekPathfinder.reachableExposedOre(
                helper.getLevel(),
                helper.absolutePos(new BlockPos(1, 2, 1)),
                Set.of(helper.absolutePos(buried),
                        helper.absolutePos(exposed)));
        helper.assertValueEqual(selected, helper.absolutePos(exposed),
                "Reachable exposed ore wins over closer sealed ore");
        helper.succeed();
    }

    @GameTest
    public void airRouteReachesOreWithoutCrossingWall(
            GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 2);
        BlockPos ore = new BlockPos(4, 1, 2);
        for (int z = 1; z <= 3; z++) {
            helper.setBlock(new BlockPos(2, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 2, z), Blocks.STONE);
        }
        helper.setBlock(ore, ModBlocks.RUBY_ORE);

        BlockPos absoluteOre = helper.absolutePos(ore);
        List<Vec3> route = SprenSeekPathfinder.route(
                helper.getLevel(),
                Vec3.atCenterOf(helper.absolutePos(start)),
                helper.absolutePos(start), absoluteOre);

        helper.assertTrue(!route.isEmpty(), "Spren has a route");
        helper.assertValueEqual(
                route.getLast(), Vec3.atCenterOf(absoluteOre),
                "Final leg reaches the ore");
        helper.assertTrue(route.size() > 1 &&
                        BlockPos.containing(route.get(route.size() - 2))
                                .distManhattan(absoluteOre) == 1,
                "Spren goes around the wall before entering the ore");
        for (Vec3 waypoint : route.subList(0, route.size() - 1)) {
            helper.assertTrue(helper.getLevel().getBlockState(
                    BlockPos.containing(waypoint)).isAir(),
                    "Trail waypoint stays in air");
        }
        helper.succeed();
    }
}

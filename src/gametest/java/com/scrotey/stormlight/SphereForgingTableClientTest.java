package com.scrotey.stormlight;

import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.client.progression.WayOfKingsScreen;
import com.scrotey.stormlight.network.OpenWayOfKingsPayload;

import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.block.entity.SphereForgingTableBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.scrotey.stormlight.block.custom.SphereForgingTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class SphereForgingTableClientTest implements FabricClientGameTest {
    private static final BlockPos POS = new BlockPos(0, 100, 0);

    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int x = -4; x <= 5; x++) for (int z = -4; z <= 5; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE_BRICKS.defaultBlockState());
                var state = ModBlocks.SPHERE_FORGING_TABLE.defaultBlockState();
                level.setBlockAndUpdate(POS, state);
                ModBlocks.SPHERE_FORGING_TABLE.setPlacedBy(level, POS, state, null, new ItemStack(ModBlocks.SPHERE_FORGING_TABLE));
                var player = world.getConnection().getServerPlayer();
                player.teleportTo(-1.5, 100, -3.5);
                player.getInventory().setItem(1, new ItemStack(ModBlocks.SPHERE_FORGING_TABLE));
                player.getInventory().setItem(2, new ItemStack(ModItems.CHIP_MOULD));
                player.getInventory().setItem(3, new ItemStack(ModItems.MARK_MOULD));
                player.getInventory().setItem(4, new ItemStack(ModItems.BROAM_MOULD));
            });
            context.runOnClient(client -> { client.player.setYRot(-34); client.player.setXRot(16); });
            world.getConnection().waitForChunksRender();
            context.waitTicks(100);
            context.takeScreenshot("forge-empty");
            view(context, world, 0.5, 102, 1.6, -121, 50);
            context.takeScreenshot("forge-from-steps");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var vat = SphereForgingTableBlock.vatInterior(POS, Direction.EAST).getCenter();
                var thrown = new ItemEntity(level, vat.x, vat.y + 0.8, vat.z, new ItemStack(Items.GLASS, 3));
                thrown.setDeltaMovement(0, 0, 0);
                thrown.setDefaultPickUpDelay();
                level.addFreshEntity(thrown);
            });
            context.waitTicks(20);
            use(world, new ItemStack(Items.LAVA_BUCKET));
            context.waitTicks(40);
            context.takeScreenshot("forge-lit-glass");
            view(context, world, -1.5, 100, -3.5, -34, 16);
            context.waitTicks(220);
            context.takeScreenshot("forge-molten");
            view(context, world, 0.5, 102, 1.6, -121, 50);
            context.takeScreenshot("forge-crucible");
            view(context, world, -1.5, 100, -3.5, -34, 16);
            use(world, new ItemStack(ModItems.MARK_MOULD));
            view(context, world, 3.0, 100, -0.6, 19, 38);
            context.takeScreenshot("forge-mould-set");
            use(world, new ItemStack(ModItems.RAW_DIAMOND_MARK));
            context.waitTicks(5);
            context.takeScreenshot("forge-gem-set");
            view(context, world, -1.5, 100, -3.5, -34, 16);
            use(world, ItemStack.EMPTY);
            context.waitTicks(18);
            context.takeScreenshot("forge-pouring");
            view(context, world, 3.2, 100, -2.2, 20, 18);
            context.takeScreenshot("forge-pouring-tap");
            context.waitTicks(32);
            context.runOnClient(client -> {
                var forge = (SphereForgingTableBlockEntity) client.level.getBlockEntity(POS);
                if (forge.getResult().isEmpty()) throw new AssertionError("Cast sphere must sync to the renderer");
            });
            context.takeScreenshot("forge-cooling");
            context.waitTicks(70);
            context.takeScreenshot("forge-finished");
            view(context, world, 4.5, 100, 4.5, 139, 15);
            context.takeScreenshot("forge-back");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35))));
            context.waitTicks(3);
            context.takeScreenshot("way-of-kings-guide");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35), 1)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-order");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35), 2)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-stone-cutting-tree");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 40), 2)));
            context.waitTicks(5);
            context.takeScreenshot("way-of-kings-combined-cutting-level-five");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35), 3)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-stormlight-guide");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 0, 0, 0, 0), 1)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-no-order");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 10, 99, 100), 2)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-mastered-tree");
        }
    }

    private static void view(ClientGameTestContext context, TestSingleplayerContext world,
                             double x, double y, double z, float yaw, float pitch) {
        world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().teleportTo(x, y, z));
        context.waitTicks(5);
        context.runOnClient(client -> { client.player.setYRot(yaw); client.player.setXRot(pitch); });
        context.waitTicks(5);
    }

    private static void use(TestSingleplayerContext world, ItemStack held) {
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var forge = (SphereForgingTableBlockEntity) server.overworld().getBlockEntity(POS);
            if (!forge.interact(player, InteractionHand.MAIN_HAND, true)) throw new AssertionError("Forge rejected " + held);
        });
    }
}

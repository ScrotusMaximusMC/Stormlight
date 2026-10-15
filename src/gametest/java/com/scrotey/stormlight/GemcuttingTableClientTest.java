package com.scrotey.stormlight;

import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.block.entity.GemcuttingTableBlockEntity;
import com.scrotey.stormlight.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class GemcuttingTableClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            BlockPos pos = new BlockPos(0, 100, 0);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int x = -4; x <= 5; x++) for (int z = -4; z <= 5; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE_BRICKS.defaultBlockState());
                var state = ModBlocks.GEMCUTTING_TABLE.defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                ModBlocks.GEMCUTTING_TABLE.setPlacedBy(level, pos, state, null, new ItemStack(ModBlocks.GEMCUTTING_TABLE));
                var player = world.getConnection().getServerPlayer();
                player.teleportTo(-1.5, 100, -2.5);
                player.setYRot(-37);
                player.setXRot(12);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 4));
            });
            context.runOnClient(client -> { client.player.setYRot(-37); client.player.setXRot(12); });
            world.getConnection().waitForChunksRender();
            context.waitTicks(160);
            context.takeScreenshot("manual-bench-empty");
            context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos.east()), Direction.NORTH, pos.east(), false)));
            context.waitTicks(30);
            context.runOnClient(client -> {
                if (client.player.containerMenu != client.player.inventoryMenu) throw new AssertionError("Bench must not open a screen");
                var table = (GemcuttingTableBlockEntity) client.level.getBlockEntity(pos);
                if (!table.getGem().is(Items.DIAMOND)) throw new AssertionError("Loaded gem must sync to renderer");
            });
            context.takeScreenshot("manual-bench-diamond");
            world.getServer().runOnServer(server -> world.getConnection().getServerPlayer()
                    .setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHISEL)));
            context.waitTicks(10);
            for (int i = 0; i < 10; i++) {
                BlockPos target = i % 2 == 0 ? pos : pos.east();
                context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(target), Direction.NORTH, target, false)));
                context.waitTicks(6);
                if (i == 7) context.takeScreenshot("manual-bench-cracked");
            }
            world.getServer().runOnServer(server -> {
                var table = (GemcuttingTableBlockEntity) server.overworld().getBlockEntity(pos);
                if (!table.getGem().isEmpty()) throw new AssertionError("Ten clicks complete cutting");
                if (world.getConnection().getServerPlayer().getMainHandItem().getDamageValue() != 10)
                    throw new AssertionError("Each click wears the held chisel");
            });
            context.waitTicks(25);
            context.takeScreenshot("manual-bench-finished");
            var gems = new net.minecraft.world.item.Item[]{ModItems.GARNET, ModItems.RUBY, ModItems.SAPPHIRE, Items.EMERALD};
            String[] names = {"garnet", "ruby", "sapphire", "emerald"};
            for (int i = 0; i < gems.length; i++) {
                final int kind = i;
                world.getServer().runOnServer(server -> {
                    var player = world.getConnection().getServerPlayer();
                    var table = (GemcuttingTableBlockEntity) server.overworld().getBlockEntity(pos);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.setShiftKeyDown(true);
                    table.interact(player, InteractionHand.MAIN_HAND);
                    player.setShiftKeyDown(false);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gems[kind]));
                    table.interact(player, InteractionHand.MAIN_HAND);
                });
                context.waitTicks(25);
                context.runOnClient(client -> {
                    var table = (GemcuttingTableBlockEntity) client.level.getBlockEntity(pos);
                    if (!table.getGem().is(gems[kind])) throw new AssertionError("Gem type must sync");
                });
                context.takeScreenshot("manual-bench-" + names[i]);
            }
        }
    }
}

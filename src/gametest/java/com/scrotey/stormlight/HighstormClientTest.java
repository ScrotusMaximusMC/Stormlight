package com.scrotey.stormlight;

import com.scrotey.stormlight.highstorm.HighstormPhase;
import com.scrotey.stormlight.highstorm.HighstormSavedData;
import com.scrotey.stormlight.highstorm.HighstormTimings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Walks a Highstorm through Gathering and Approach, screenshotting the
 * stormwall from several angles and checking the sky stays dry until the
 * wall's leading edge has passed overhead.
 */
public class HighstormClientTest implements FabricClientGameTest {
    private static final float EAST = -90.0F;

    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var source = server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(source, "time set 1000");
                server.getCommands().performPrefixedCommand(source, "gamemode creative Player0");
                var player = world.getConnection().getServerPlayer();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(0.5, -50, 0.5);
            });
            look(context, EAST, -6);
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);

            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.02F);
            context.takeScreenshot("highstorm-gathering-02-east");
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.05F);
            context.takeScreenshot("highstorm-gathering-05-east");
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.10F);
            context.takeScreenshot("highstorm-gathering-10-east");
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.40F);
            context.takeScreenshot("highstorm-gathering-40-east");
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.95F);
            context.takeScreenshot("highstorm-gathering-95-east");
            look(context, EAST - 70, -6);
            context.takeScreenshot("highstorm-gathering-95-north-east");
            look(context, EAST + 70, -6);
            context.takeScreenshot("highstorm-gathering-95-south-east");

            world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().teleportTo(0.5, 140, 0.5));
            look(context, EAST, 8);
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.25F);
            context.takeScreenshot("highstorm-gathering-25-elevated");
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.50F);
            context.takeScreenshot("highstorm-gathering-50-elevated");
            phase(context, world, HighstormPhase.GATHERING, HighstormTimings.GATHERING_TICKS, 0.95F);
            look(context, EAST, 35);
            context.takeScreenshot("highstorm-gathering-95-from-above");
            world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().teleportTo(0.5, -50, 0.5));

            look(context, EAST, -6);
            phase(context, world, HighstormPhase.APPROACHING, HighstormTimings.APPROACHING_TICKS, 0.30F);
            context.takeScreenshot("highstorm-approach-30-east");
            phase(context, world, HighstormPhase.APPROACHING, HighstormTimings.APPROACHING_TICKS, 0.70F);
            context.takeScreenshot("highstorm-approach-70-east");
            look(context, 180, -6);
            context.takeScreenshot("highstorm-approach-70-north");
            look(context, 90, -6);
            context.takeScreenshot("highstorm-approach-70-west");
            look(context, EAST, -6);
            phase(context, world, HighstormPhase.APPROACHING, HighstormTimings.APPROACHING_TICKS, 0.90F);
            context.takeScreenshot("highstorm-approach-90-east");
            context.runOnClient(client -> {
                if (client.level.isRaining()) throw new AssertionError("Rain must not start before the stormwall arrives");
            });

            // The Highstorm begins as the first cloud is overhead, then the wall rolls on past.
            look(context, EAST, -20);
            phase(context, world, HighstormPhase.APPROACHING, HighstormTimings.APPROACHING_TICKS, 0.99F);
            context.takeScreenshot("highstorm-transition-1-approach-99");
            phase(context, world, HighstormPhase.HIGHSTORM, HighstormTimings.HIGHSTORM_TICKS, 0.04F);
            context.takeScreenshot("highstorm-transition-2-wall-rolling-over");
            look(context, 90, -20);
            context.takeScreenshot("highstorm-transition-3-wall-rolling-over-west");
            look(context, EAST, -20);
            phase(context, world, HighstormPhase.HIGHSTORM, HighstormTimings.HIGHSTORM_TICKS, 0.13F);
            context.takeScreenshot("highstorm-transition-4-crossfade");
            context.runOnClient(client -> {
                if (!client.level.isRaining()) throw new AssertionError("Rain must be falling once the Highstorm has begun");
            });
            phase(context, world, HighstormPhase.HIGHSTORM, HighstormTimings.HIGHSTORM_TICKS, 0.20F);
            context.takeScreenshot("highstorm-transition-5-active");

            // The departing wall's curtain should sweep past without a hard edge.
            phase(context, world, HighstormPhase.PASSING, HighstormTimings.PASSING_TICKS, 0.38F);
            context.takeScreenshot("highstorm-passing-1-curtain-near");
            phase(context, world, HighstormPhase.PASSING, HighstormTimings.PASSING_TICKS, 0.44F);
            context.takeScreenshot("highstorm-passing-2-curtain-overhead");
            phase(context, world, HighstormPhase.PASSING, HighstormTimings.PASSING_TICKS, 0.52F);
            context.takeScreenshot("highstorm-passing-3-curtain-past");

        }
    }

    private static void look(ClientGameTestContext context, float yaw, float pitch) {
        context.runOnClient(client -> {
            client.player.setYRot(yaw);
            client.player.setXRot(pitch);
        });
        context.waitTicks(5);
    }

    private static void phase(ClientGameTestContext context, TestSingleplayerContext world,
                              HighstormPhase phase, int length, float progress) {
        world.getServer().runOnServer(server -> HighstormSavedData.get(server)
                .setPhase(phase, Math.round(length * (1.0F - progress))));
        context.waitTicks(30);
    }
}

package com.scrotey.stormlight.worldgen.chrysalis;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;

public final class ChrysalisCommands {
    private ChrysalisCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("chrysalis")
                                        .requires(source ->
                                                source.permissions()
                                                        .hasPermission(
                                                                Permissions.COMMANDS_MODERATOR
                                                        )
                                        )
                                        .then(
                                                Commands.literal("locate")
                                                        .executes(ChrysalisCommands::locate)
                                        )
                                        .then(
                                                Commands.literal("status")
                                                        .executes(ChrysalisCommands::status)
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        ChrysalisSavedData data =
                ChrysalisSavedData.get(source.getServer());

        data.pruneMissingGemhearts(source.getLevel());

        String status =
                ChrysalisSpawner.getDebugStatus(
                        source.getServer()
                );

        source.sendSuccess(
                () -> Component.literal(status),
                false
        );

        return 1;
    }

    private static int locate(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        ChrysalisSavedData data = ChrysalisSavedData.get(source.getServer());

        // Clean up any tracked Gemhearts that were removed by commands or
        // other non-mining actions, but only where their chunks are loaded.
        data.pruneMissingGemhearts(source.getLevel());

        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos nearest = data.findNearestActiveGemheart(origin);

        if (nearest == null) {
            source.sendFailure(
                    Component.literal(
                            "No active chrysalis with an unmined Gemheart is currently tracked."
                    )
            );
            return 0;
        }

        long dx = (long) nearest.getX() - origin.getX();
        long dz = (long) nearest.getZ() - origin.getZ();
        int distance = Mth.floor(Math.sqrt((double) (dx * dx + dz * dz)));

        source.sendSuccess(
                () -> Component.literal(
                        "Nearest active chrysalis: ["
                                + nearest.getX()
                                + ", "
                                + nearest.getY()
                                + ", "
                                + nearest.getZ()
                                + "] (about "
                                + distance
                                + " blocks away)"
                ),
                false
        );

        return 1;
    }
}

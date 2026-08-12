package com.scrotey.stormlight.highstorm;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class HighstormCommands {
    private HighstormCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("highstorm")
                                        .requires(source ->
                                                source.permissions()
                                                        .hasPermission(
                                                                Permissions.COMMANDS_MODERATOR
                                                        )
                                        )
                                        .executes(HighstormCommands::status)
                                        .then(
                                                Commands.literal("start")
                                                        .executes(HighstormCommands::start)
                                        )
                                        .then(
                                                Commands.literal("stop")
                                                        .executes(HighstormCommands::stop)
                                        )
                                        .then(
                                                Commands.literal("approach")
                                                        .executes(
                                                                HighstormCommands::approach
                                                        )
                                        )
                                        .then(
                                                Commands.literal("passing")
                                                        .executes(
                                                                HighstormCommands::passing
                                                        )
                                        )
                                        .then(
                                                Commands.literal("status")
                                                        .executes(HighstormCommands::status)
                                        )
                        )
        );
    }

    private static int start(
            CommandContext<CommandSourceStack> context
    ) {
        HighstormManager.startNow(
                context.getSource().getServer()
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Started a five-minute Highstorm."
                ),
                true
        );

        return 1;
    }

    private static int stop(
            CommandContext<CommandSourceStack> context
    ) {
        HighstormManager.stopNow(
                context.getSource().getServer()
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Stopped the Highstorm and scheduled the next one."
                ),
                true
        );

        return 1;
    }

    private static int approach(
            CommandContext<CommandSourceStack> context
    ) {
        HighstormManager.startApproachNow(
                context.getSource().getServer()
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Started the three-minute Highstorm approach."
                ),
                true
        );

        return 1;
    }

    private static int passing(
            CommandContext<CommandSourceStack> context
    ) {
        HighstormManager.startPassingNow(
                context.getSource().getServer()
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Started the three-minute Highstorm passing phase."
                ),
                true
        );

        return 1;
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {
        context.getSource().sendSuccess(
                () -> HighstormManager.getStatus(
                        context.getSource().getServer()
                ),
                false
        );

        return 1;
    }
}

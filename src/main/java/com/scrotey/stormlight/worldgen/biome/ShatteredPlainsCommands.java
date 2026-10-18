package com.scrotey.stormlight.worldgen.biome;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

import org.jspecify.annotations.Nullable;

/*
 * /shatteredplains locate searches much further than vanilla's
 * /locate biome, which stops at 6400 blocks; the plains are rare enough
 * that the nearest is often beyond that.
 */
public final class ShatteredPlainsCommands {
    private static final int SEARCH_RADIUS = 32_000;
    private static final int SEARCH_STEP = 128;
    private static final int DEEP_RADIUS = 400;

    private ShatteredPlainsCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("shatteredplains")
                                        .requires(source ->
                                                source.permissions()
                                                        .hasPermission(
                                                                Permissions.COMMANDS_MODERATOR
                                                        )
                                        )
                                        .then(
                                                Commands.literal("locate")
                                                        .executes(ShatteredPlainsCommands::locate)
                                        )
                                        .then(
                                                Commands.literal("here")
                                                        .executes(ShatteredPlainsCommands::here)
                                        )
                        )
        );
    }

    private static int locate(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        var chunkSource = level.getChunkSource();

        if (!chunkSource.getGenerator().getBiomeSource().possibleBiomes().stream()
                .anyMatch(biome -> biome.is(ShatteredPlains.BIOME))) {
            source.sendFailure(Component.literal("The Shattered Plains don't generate in this dimension."));
            return 0;
        }

        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos pos = findDeepPlains(level, origin);

        if (pos == null) {
            source.sendFailure(Component.literal(
                    "No Shattered Plains within " + SEARCH_RADIUS + " blocks."));
            return 0;
        }

        long dx = (long) pos.getX() - origin.getX();
        long dz = (long) pos.getZ() - origin.getZ();
        int distance = Mth.floor(Math.sqrt((double) (dx * dx + dz * dz)));
        String teleport = "/tp @s " + pos.getX() + " 200 " + pos.getZ();

        source.sendSuccess(
                () -> Component.literal("Nearest Shattered Plains: ")
                        .append(Component.literal("[" + pos.getX() + ", ~, " + pos.getZ() + "]")
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.GREEN)
                                        .withClickEvent(new ClickEvent.SuggestCommand(teleport))))
                        .append(" (" + distance + " blocks away)"),
                false
        );
        return 1;
    }

    /* Biome under the player, and whether chunk generation agrees with the biome source. */
    private static int here(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Holder<Biome> generated = level.getBiome(pos);
        boolean predicted = isPlains(level, pos.getX(), pos.getZ());
        boolean deep = isDeep(level, pos.getX(), pos.getZ());

        source.sendSuccess(
                () -> Component.literal("Biome here: "
                        + generated.unwrapKey().map(key -> key.identifier().toString()).orElse("?")
                        + (predicted ? " | inside the Shattered Plains" : " | outside the Shattered Plains")
                        + (deep ? " (deep inside, full plateau terrain)" : "")),
                false
        );
        return 1;
    }

    private static boolean isPlains(ServerLevel level, int blockX, int blockZ) {
        var chunkSource = level.getChunkSource();
        return chunkSource.getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(blockX), QuartPos.fromBlock(level.getSeaLevel() + 32),
                QuartPos.fromBlock(blockZ), chunkSource.randomState().sampler()
        ).is(ShatteredPlains.BIOME);
    }

    /* Plains for DEEP_RADIUS blocks all round, which puts it past the terrain blend at the edge. */
    private static boolean isDeep(ServerLevel level, int blockX, int blockZ) {
        if (!isPlains(level, blockX, blockZ)) {
            return false;
        }
        for (int a = 0; a < 8; a++) {
            double angle = a * Math.PI / 4;
            if (!isPlains(level, blockX + (int) (DEEP_RADIUS * Math.cos(angle)),
                    blockZ + (int) (DEEP_RADIUS * Math.sin(angle)))) {
                return false;
            }
        }
        return true;
    }

    /* Searches outwards in square rings for the nearest deep point. */
    private static @Nullable BlockPos findDeepPlains(ServerLevel level, BlockPos origin) {
        int rings = SEARCH_RADIUS / SEARCH_STEP;
        for (int ring = 0; ring <= rings; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.abs(i) != ring && Math.abs(j) != ring) {
                        continue;
                    }
                    int x = origin.getX() + i * SEARCH_STEP;
                    int z = origin.getZ() + j * SEARCH_STEP;
                    if (isDeep(level, x, z)) {
                        return new BlockPos(x, level.getSeaLevel(), z);
                    }
                }
            }
        }
        return null;
    }
}

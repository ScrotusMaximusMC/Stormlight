package com.scrotey.stormlight.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.network.OpenWayOfKingsPayload;
import com.scrotey.stormlight.progression.RadiantProgression;
import com.scrotey.stormlight.progression.StonecuttingProgression;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

/** Operator-only shortcuts for manually checking the guide and sphere visuals. */
public final class DebugUiCommands {
    private DebugUiCommands() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var root = Commands.literal("stormlightdebug")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "/stormlightdebug guide [gem_level] [radiant_level] | spheres <chip|mark|broam> [charge_percent] | wear <percent> | gemlevel <level> | books"), false);
                        return 1;
                    });
            root.then(Commands.literal("guide")
                    .executes(ctx -> guide(ctx.getSource(), -1, -1))
                    .then(Commands.argument("gem_level", IntegerArgumentType.integer(1, 10))
                            .executes(ctx -> guide(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "gem_level"), -1))
                            .then(Commands.argument("radiant_level", IntegerArgumentType.integer(0, 10))
                                    .executes(ctx -> guide(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "gem_level"),
                                            IntegerArgumentType.getInteger(ctx, "radiant_level"))))));
            var spheres = Commands.literal("spheres");
            for (String size : new String[]{"chip", "mark", "broam"}) spheres.then(samples(size));
            root.then(spheres);
            root.then(Commands.literal("wear").then(Commands.argument("percent", IntegerArgumentType.integer(0, 99))
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        var held = player.getMainHandItem();
                        if (!(held.getItem() instanceof SphereItem)) {
                            ctx.getSource().sendFailure(Component.literal("Hold a sphere in your main hand first."));
                            return 0;
                        }
                        int percent = IntegerArgumentType.getInteger(ctx, "percent");
                        held.setDamageValue((int) Math.ceil(held.getMaxDamage() * percent / 100.0));
                        player.getInventory().setChanged();
                        player.containerMenu.broadcastChanges();
                        ctx.getSource().sendSuccess(() -> Component.literal("Held sphere wear set to " + percent + "% (charge unchanged)."), false);
                        return 1;
                    })));
            root.then(Commands.literal("gemlevel").then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                    .executes(ctx -> {
                        int level = IntegerArgumentType.getInteger(ctx, "level");
                        ctx.getSource().getPlayerOrException().setAttached(StonecuttingProgression.COMPLETED_CUTS,
                                StonecuttingProgression.cutsForLevel(level));
                        ctx.getSource().sendSuccess(() -> Component.literal("Gem Cutting set to level " + level + ". Saved progress has changed."), false);
                        return 1;
                    })));
            root.then(Commands.literal("books").executes(ctx -> {
                var player = ctx.getSource().getPlayerOrException();
                for (var item : new net.minecraft.world.item.Item[]{ModItems.WAY_OF_KINGS, ModItems.MYSTERIOUS_BOOK, ModItems.WORDS_OF_RADIANCE})
                    player.getInventory().placeItemBackInInventory(new ItemStack(item));
                ctx.getSource().sendSuccess(() -> Component.literal("Given all three books for lectern testing."), false);
                return 1;
            }));
            dispatcher.register(root);
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> samples(String size) {
        return Commands.literal(size).executes(ctx -> giveSamples(ctx.getSource(), size, 100))
                .then(Commands.argument("charge_percent", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> giveSamples(ctx.getSource(), size, IntegerArgumentType.getInteger(ctx, "charge_percent"))));
    }

    private static int giveSamples(CommandSourceStack source, String size, int charge) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        for (String gem : new String[]{"diamond", "garnet", "ruby", "sapphire", "emerald"}) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("stormlight", gem + "_" + size));
            for (int wear : new int[]{0, 10, 50, 95}) {
                var stack = new ItemStack(item);
                var sphere = (SphereItem) item;
                sphere.setCharge(stack, sphere.getCapacity() * charge / 100);
                stack.setDamageValue(stack.getMaxDamage() * wear / 100);
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        source.sendSuccess(() -> Component.literal("Given 20 " + size + " spheres: five gemstones at 0/10/50/95% wear, " + charge + "% charge. Overflow drops at your feet."), false);
        return 20;
    }

    private static int guide(CommandSourceStack source, int gemLevel, int radiantLevel) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int radiant = radiantLevel < 0 ? RadiantProgression.getLevel(player) : radiantLevel;
        int order = radiantLevel < 0 ? RadiantProgression.getOrderNetworkId(player) : radiant > 0 ? 1 : 0;
        ServerPlayNetworking.send(player, new OpenWayOfKingsPayload(player.blockPosition(), order, radiant,
                player.experienceLevel, gemLevel < 0 ? StonecuttingProgression.getCompletedCuts(player)
                        : StonecuttingProgression.cutsForLevel(gemLevel)));
        source.sendSuccess(() -> Component.literal("Guide preview opened. Saved progression is unchanged."), false);
        return 1;
    }
}

package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SphereItem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

public final class SphereDecayManager {

    /*
     * Checking once per second is plenty. Exact elapsed world time
     * is stored on each sphere, so this does not reduce accuracy.
     */
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final Set<BlockEntity>
            LOADED_CONTAINERS =
            new CopyOnWriteArraySet<>();

    private SphereDecayManager() {
    }

    public static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(
                (blockEntity, level) -> {
                    if (blockEntity instanceof Container) {
                        LOADED_CONTAINERS.add(
                                blockEntity
                        );
                    }
                }
        );

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(
                (blockEntity, level) ->
                        LOADED_CONTAINERS.remove(
                                blockEntity
                        )
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server ->
                        LOADED_CONTAINERS.clear()
        );

        ServerTickEvents.END_SERVER_TICK.register(
                SphereDecayManager::tick
        );
    }

    private static void tick(
            MinecraftServer server
    ) {
        long currentGameTick =
                server.overworld()
                        .getGameTime();

        if (currentGameTick
                % CHECK_INTERVAL_TICKS != 0L) {

            return;
        }

        for (ServerLevel level
                : server.getAllLevels()) {

            decayPlayerSpheres(
                    level,
                    currentGameTick
            );

            decayLoadedContainers(
                    level,
                    currentGameTick
            );

            decayDroppedItems(
                    level,
                    currentGameTick
            );
        }
    }

    private static void decayPlayerSpheres(
            ServerLevel level,
            long currentGameTick
    ) {
        for (ServerPlayer player
                : level.players()) {

            boolean inventoryChanged =
                    false;

            for (ItemStack stack
                    : player.getInventory()
                    .getNonEquipmentItems()) {

                if (decayStack(
                        stack,
                        currentGameTick
                )) {
                    inventoryChanged = true;
                }
            }

            if (decayStack(
                    player.getOffhandItem(),
                    currentGameTick
            )) {
                inventoryChanged = true;
            }

            if (inventoryChanged) {
                player.getInventory()
                        .setChanged();
            }

            decayContainer(
                    player.getEnderChestInventory(),
                    currentGameTick
            );

            decayPlayerSphereStorage(
                    player,
                    currentGameTick
            );
        }
    }

    private static void decayPlayerSphereStorage(
            ServerPlayer player,
            long currentGameTick
    ) {
        NonNullList<ItemStack> sphereItems =
                ModAttachments.getSphereItems(
                        player
                );

        boolean changed = false;

        /*
         * Decay continues even while the pouch is unequipped. The
         * spheres still exist; they are merely inaccessible.
         */
        for (ItemStack sphereStack
                : sphereItems) {

            if (decayStack(
                    sphereStack,
                    currentGameTick
            )) {
                changed = true;
            }
        }

        if (changed) {
            ModAttachments.setSphereItems(
                    player,
                    sphereItems
            );
        }
    }

    private static void decayLoadedContainers(
            ServerLevel level,
            long currentGameTick
    ) {
        for (BlockEntity blockEntity
                : LOADED_CONTAINERS) {

            if (blockEntity.getLevel() != level
                    || blockEntity.isRemoved()
                    || !(blockEntity
                    instanceof Container container)) {

                continue;
            }

            decayContainer(
                    container,
                    currentGameTick
            );
        }
    }

    private static void decayContainer(
            Container container,
            long currentGameTick
    ) {
        boolean changed = false;

        for (int slot = 0;
             slot < container.getContainerSize();
             slot++) {

            if (decayStack(
                    container.getItem(slot),
                    currentGameTick
            )) {
                changed = true;
            }
        }

        if (changed) {
            container.setChanged();
        }
    }

    private static void decayDroppedItems(
            ServerLevel level,
            long currentGameTick
    ) {
        for (ItemEntity itemEntity
                : level.getEntities(
                EntityTypeTest.forClass(
                        ItemEntity.class
                ),
                itemEntity -> true
        )) {

            ItemStack stack =
                    itemEntity.getItem();

            if (decayStack(
                    stack,
                    currentGameTick
            )) {
                itemEntity.setItem(
                        stack.copy()
                );
            }
        }
    }

    /**
     * Applies passive decay to a sphere stack.
     */
    /**
     * Applies passive decay to a sphere stack.
     */
    private static boolean decayStack(
            ItemStack stack,
            long currentGameTick
    ) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem()
                instanceof SphereItem sphere) {

            return sphere.applyPassiveDecay(
                    stack,
                    currentGameTick
            );
        }

        return false;
    }
}
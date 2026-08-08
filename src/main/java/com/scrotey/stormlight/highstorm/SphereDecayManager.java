package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.item.SpherePouchItem;

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
import net.minecraft.world.item.component.ItemContainerContents;
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

            decayEquippedPouch(
                    player,
                    currentGameTick
            );
        }
    }

    private static void decayEquippedPouch(
            ServerPlayer player,
            long currentGameTick
    ) {
        ItemStack pouch =
                ModAttachments.getEquippedPouch(
                        player
                );

        if (pouch.isEmpty()) {
            return;
        }

        if (decayStack(
                pouch,
                currentGameTick
        )) {
            ModAttachments.setEquippedPouch(
                    player,
                    pouch
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
     * Decays either a sphere itself or all spheres stored inside a
     * Sphere Pouch.
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

        if (stack.getItem()
                instanceof SpherePouchItem pouchItem) {

            return decayPouchContents(
                    stack,
                    pouchItem,
                    currentGameTick
            );
        }

        return false;
    }

    private static boolean decayPouchContents(
            ItemStack pouch,
            SpherePouchItem pouchItem,
            long currentGameTick
    ) {
        NonNullList<ItemStack> pouchItems =
                NonNullList.withSize(
                        SpherePouchItem.SLOT_COUNT,
                        ItemStack.EMPTY
                );

        pouchItem.getContents(pouch)
                .copyInto(pouchItems);

        boolean changed = false;

        for (ItemStack stack : pouchItems) {
            if (decayStack(
                    stack,
                    currentGameTick
            )) {
                changed = true;
            }
        }

        if (changed) {
            pouchItem.setContents(
                    pouch,
                    ItemContainerContents.fromItems(
                            pouchItems
                    )
            );
        }

        return changed;
    }
}
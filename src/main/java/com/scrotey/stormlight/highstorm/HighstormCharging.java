package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;
import com.scrotey.stormlight.item.SphereItem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.Set;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

final class HighstormCharging {
    private static final int TICKS_PER_SECOND = 20;

    /*
     * This provides the common number of charging steps. Sphere
     * capacities scale their gain proportionally against this value.
     */
    private static final int MAX_SPHERE_CAPACITY = 200;

    private static final int
            SPHERE_JAR_FULL_CHARGE_SECONDS = 120;

    private static final int
            HELD_AND_DROPPED_FULL_CHARGE_SECONDS = 300;

    private static final int
            INVENTORY_FULL_CHARGE_SECONDS = 450;

    private static final int
            VANILLA_STORAGE_FULL_CHARGE_SECONDS = 600;

    private static final int
            HELD_AND_DROPPED_INTERVAL_TICKS =
            calculateBaseInterval(
                    HELD_AND_DROPPED_FULL_CHARGE_SECONDS
            );

    private static final int
            INVENTORY_INTERVAL_TICKS =
            calculateBaseInterval(
                    INVENTORY_FULL_CHARGE_SECONDS
            );

    private static final int
            VANILLA_STORAGE_INTERVAL_TICKS =
            calculateBaseInterval(
                    VANILLA_STORAGE_FULL_CHARGE_SECONDS
            );

    /*
     * CopyOnWriteArraySet avoids the ConcurrentModificationException
     * previously encountered when block entities load or unload.
     */
    private static final Set<BlockEntity>
            LOADED_CONTAINERS =
            new CopyOnWriteArraySet<>();

    private HighstormCharging() {
    }

    static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(
                (blockEntity, level) -> {
                    if (blockEntity
                            instanceof Container) {

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
    }

    static void tick(
            ServerLevel level,
            long elapsedHighstormTicks
    ) {
        /*
         * Sphere Jars use exact elapsed time because their audiovisual
         * effects also need to be evaluated every tick.
         */
        chargeExposedSphereJars(
                level,
                elapsedHighstormTicks
        );

        if (elapsedHighstormTicks
                % HELD_AND_DROPPED_INTERVAL_TICKS
                == 0L) {

            long pulseNumber =
                    elapsedHighstormTicks
                            / HELD_AND_DROPPED_INTERVAL_TICKS;

            for (ServerPlayer player
                    : level.players()) {

                chargeExposedHeldSpheres(
                        level,
                        player,
                        pulseNumber
                );
            }

            chargeExposedDroppedSpheres(
                    level,
                    pulseNumber
            );
        }

        if (elapsedHighstormTicks
                % INVENTORY_INTERVAL_TICKS
                == 0L) {

            long pulseNumber =
                    elapsedHighstormTicks
                            / INVENTORY_INTERVAL_TICKS;

            chargeExposedPlayerInventories(
                    level,
                    pulseNumber
            );
        }

        if (elapsedHighstormTicks
                % VANILLA_STORAGE_INTERVAL_TICKS
                == 0L) {

            long pulseNumber =
                    elapsedHighstormTicks
                            / VANILLA_STORAGE_INTERVAL_TICKS;

            chargeExposedPlayerSphereStorage(
                    level,
                    pulseNumber
            );

            chargeExposedContainers(
                    level,
                    pulseNumber
            );
        }
    }

    private static int calculateBaseInterval(
            int fullChargeSeconds
    ) {
        return Math.max(
                1,
                fullChargeSeconds
                        * TICKS_PER_SECOND
                        / MAX_SPHERE_CAPACITY
        );
    }

    private static void chargeExposedSphereJars(
            ServerLevel level,
            long elapsedHighstormTicks
    ) {
        for (BlockEntity blockEntity
                : LOADED_CONTAINERS) {

            if (blockEntity.getLevel() != level
                    || blockEntity.isRemoved()
                    || !(blockEntity
                    instanceof SphereJarBlockEntity sphereJar)) {

                continue;
            }

            BlockPos skyCheckPosition =
                    sphereJar.getBlockPos()
                            .above();

            if (!level.canSeeSky(
                    skyCheckPosition
            )) {
                continue;
            }

            boolean changed = false;

            for (int slot = 0;
                 slot < sphereJar.getContainerSize();
                 slot++) {

                ItemStack originalStack =
                        sphereJar.getItem(slot);

                if (!(originalStack.getItem()
                        instanceof SphereItem)) {

                    continue;
                }

                ItemStack updatedStack =
                        originalStack.copy();

                if (chargeSphereOverDuration(
                        updatedStack,
                        elapsedHighstormTicks,
                        SPHERE_JAR_FULL_CHARGE_SECONDS
                )) {
                    sphereJar.setItem(
                            slot,
                            updatedStack
                    );

                    changed = true;
                }
            }

            if (changed) {
                sphereJar.setChanged();
            }

            SphereJarChargingEffects.tick(
                    level,
                    sphereJar,
                    elapsedHighstormTicks
            );
        }
    }

    private static void chargeExposedHeldSpheres(
            ServerLevel level,
            ServerPlayer player,
            long pulseNumber
    ) {
        if (!level.canSeeSky(
                player.blockPosition()
        )) {
            return;
        }

        chargeSphere(
                player.getMainHandItem(),
                pulseNumber
        );

        chargeSphere(
                player.getOffhandItem(),
                pulseNumber
        );
    }

    private static void chargeExposedDroppedSpheres(
            ServerLevel level,
            long pulseNumber
    ) {
        for (ItemEntity itemEntity
                : getExposedDroppedItems(level)) {

            ItemStack originalStack =
                    itemEntity.getItem();

            if (!(originalStack.getItem()
                    instanceof SphereItem)) {

                continue;
            }

            ItemStack updatedStack =
                    originalStack.copy();

            if (chargeSphere(
                    updatedStack,
                    pulseNumber
            )) {
                itemEntity.setItem(
                        updatedStack
                );
            }
        }
    }

    private static void chargeExposedPlayerInventories(
            ServerLevel level,
            long pulseNumber
    ) {
        for (ServerPlayer player
                : level.players()) {

            if (!level.canSeeSky(
                    player.blockPosition()
            )) {
                continue;
            }

            var inventory =
                    player.getInventory();

            var items =
                    inventory.getNonEquipmentItems();

            int selectedSlot =
                    inventory.getSelectedSlot();

            boolean changed = false;

            for (int slot = 0;
                 slot < items.size();
                 slot++) {

                /*
                 * The selected loose sphere already receives the faster
                 * held-item rate.
                 */
                if (slot == selectedSlot) {
                    continue;
                }

                if (chargeSphere(
                        items.get(slot),
                        pulseNumber
                )) {
                    changed = true;
                }
            }

            if (changed) {
                inventory.setChanged();
            }
        }
    }

    private static void chargeExposedPlayerSphereStorage(
            ServerLevel level,
            long pulseNumber
    ) {
        for (ServerPlayer player
                : level.players()) {

            if (!level.canSeeSky(
                    player.blockPosition()
            )) {
                continue;
            }

            /*
             * Hidden spheres cannot charge unless the pouch that grants
             * access to them is currently equipped.
             */
            if (!ModAttachments
                    .hasEquippedPouch(player)) {

                continue;
            }

            var sphereItems =
                    ModAttachments.getSphereItems(
                            player
                    );

            boolean changed = false;

            for (ItemStack sphereStack
                    : sphereItems) {

                if (chargeSphere(
                        sphereStack,
                        pulseNumber
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
    }

    private static void chargeExposedContainers(
            ServerLevel level,
            long pulseNumber
    ) {
        for (BlockEntity blockEntity
                : LOADED_CONTAINERS) {

            if (blockEntity.getLevel() != level
                    || blockEntity.isRemoved()
                    || blockEntity
                    instanceof SphereJarBlockEntity
                    || !(blockEntity
                    instanceof Container container)) {

                continue;
            }

            BlockPos skyCheckPosition =
                    blockEntity.getBlockPos()
                            .above();

            if (!level.canSeeSky(
                    skyCheckPosition
            )) {
                continue;
            }

            boolean changed = false;

            for (int slot = 0;
                 slot < container.getContainerSize();
                 slot++) {

                ItemStack originalStack =
                        container.getItem(slot);

                ItemStack updatedStack =
                        originalStack.copy();

                if (!chargeVanillaStorageStack(
                        updatedStack,
                        pulseNumber
                )) {
                    continue;
                }

                container.setItem(
                        slot,
                        updatedStack
                );

                changed = true;
            }

            if (changed) {
                container.setChanged();
            }
        }
    }

    private static List<? extends ItemEntity>
    getExposedDroppedItems(
            ServerLevel level
    ) {
        return level.getEntities(
                EntityTypeTest.forClass(
                        ItemEntity.class
                ),
                itemEntity ->
                        level.canSeeSky(
                                itemEntity.blockPosition()
                        )
        );
    }

    private static boolean chargeVanillaStorageStack(
            ItemStack stack,
            long pulseNumber
    ) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem()
                instanceof SphereItem) {

            return chargeSphere(
                    stack,
                    pulseNumber
            );
        }

        return false;
    }

    private static boolean chargeSphereOverDuration(
            ItemStack stack,
            long elapsedTicks,
            int fullChargeSeconds
    ) {
        if (!(stack.getItem()
                instanceof SphereItem sphere)) {

            return false;
        }

        int capacity =
                sphere.getCapacity();

        int currentCharge =
                sphere.getCharge(stack);

        if (currentCharge >= capacity) {
            return false;
        }

        long fullChargeTicks =
                (long) fullChargeSeconds
                        * TICKS_PER_SECOND;

        long currentProgress =
                elapsedTicks
                        * capacity
                        / fullChargeTicks;

        long previousProgress =
                (elapsedTicks - 1L)
                        * capacity
                        / fullChargeTicks;

        int chargeToAdd =
                (int) (
                        currentProgress
                                - previousProgress
                );

        if (chargeToAdd <= 0) {
            return false;
        }

        sphere.setCharge(
                stack,
                Math.min(
                        capacity,
                        currentCharge
                                + chargeToAdd
                )
        );

        return true;
    }

    private static boolean chargeSphere(
            ItemStack stack,
            long pulseNumber
    ) {
        if (!(stack.getItem()
                instanceof SphereItem sphere)) {

            return false;
        }

        int capacity =
                sphere.getCapacity();

        int currentCharge =
                sphere.getCharge(stack);

        if (currentCharge >= capacity) {
            return false;
        }

        long currentProgress =
                pulseNumber
                        * capacity
                        / MAX_SPHERE_CAPACITY;

        long previousProgress =
                (pulseNumber - 1L)
                        * capacity
                        / MAX_SPHERE_CAPACITY;

        if (currentProgress
                <= previousProgress) {

            return false;
        }

        int chargeIncrease =
                (int) (
                        currentProgress
                                - previousProgress
                );

        sphere.setCharge(
                stack,
                currentCharge
                        + chargeIncrease
        );

        return true;
    }
}
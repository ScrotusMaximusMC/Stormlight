package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;
import com.scrotey.stormlight.item.SphereItem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.HashSet;
import java.util.Set;

public final class HighstormManager {
    private static final int TICKS_PER_SECOND = 20;

    // Base number of charging steps used by Diamond spheres.
    private static final int MAX_SPHERE_CAPACITY = 200;

    // Change these values whenever you want to rebalance charging.
    private static final int SPHERE_JAR_FULL_CHARGE_SECONDS = 12;
    private static final int HELD_AND_DROPPED_FULL_CHARGE_SECONDS = 30;
    private static final int INVENTORY_FULL_CHARGE_SECONDS = 45;
    private static final int VANILLA_STORAGE_FULL_CHARGE_SECONDS = 60;

    private static final int HELD_AND_DROPPED_INTERVAL_TICKS =
            calculateBaseInterval(
                    HELD_AND_DROPPED_FULL_CHARGE_SECONDS
            );

    private static final int INVENTORY_INTERVAL_TICKS =
            calculateBaseInterval(
                    INVENTORY_FULL_CHARGE_SECONDS
            );

    private static final int VANILLA_STORAGE_INTERVAL_TICKS =
            calculateBaseInterval(
                    VANILLA_STORAGE_FULL_CHARGE_SECONDS
            );

    private static long highstormTickCounter = 0;
    private static boolean highstormWasActive = false;
    private static final Set<BlockEntity> LOADED_CONTAINERS =
            new HashSet<>();

    private HighstormManager() {
    }

    public static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(
                (blockEntity, level) -> {
                    if (blockEntity instanceof Container) {
                        LOADED_CONTAINERS.add(blockEntity);
                    }
                }
        );

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(
                (blockEntity, level) ->
                        LOADED_CONTAINERS.remove(blockEntity)
        );

        ServerTickEvents.END_SERVER_TICK.register(
                HighstormManager::tick
        );
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

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        boolean highstormActive = level.isThundering();

        if (highstormActive && !highstormWasActive) {
            announce(
                    server,
                    "A Highstorm has arrived!",
                    ChatFormatting.AQUA
            );
        } else if (!highstormActive && highstormWasActive) {
            announce(
                    server,
                    "The Highstorm has passed.",
                    ChatFormatting.GRAY
            );
        }

        highstormWasActive = highstormActive;

        if (!highstormActive) {
            highstormTickCounter = 0;
            return;
        }

        highstormTickCounter++;

        chargeExposedSphereJars(
                level,
                highstormTickCounter
        );

        if (highstormTickCounter
                % HELD_AND_DROPPED_INTERVAL_TICKS == 0) {

            long pulseNumber =
                    highstormTickCounter
                            / HELD_AND_DROPPED_INTERVAL_TICKS;

            for (ServerPlayer player : level.players()) {
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

        if (highstormTickCounter
                % INVENTORY_INTERVAL_TICKS == 0) {

            long pulseNumber =
                    highstormTickCounter
                            / INVENTORY_INTERVAL_TICKS;

            chargeExposedPlayerInventories(
                    level,
                    pulseNumber
            );
        }

        if (highstormTickCounter
                % VANILLA_STORAGE_INTERVAL_TICKS == 0) {

            long pulseNumber =
                    highstormTickCounter
                            / VANILLA_STORAGE_INTERVAL_TICKS;

            chargeExposedContainers(
                    level,
                    pulseNumber
            );
        }
    }

    private static void chargeExposedSphereJars(
            ServerLevel level,
            long elapsedHighstormTicks
    ) {
        for (BlockEntity blockEntity : LOADED_CONTAINERS) {
            if (blockEntity.getLevel() != level
                    || blockEntity.isRemoved()
                    || !(blockEntity
                    instanceof SphereJarBlockEntity sphereJar)) {
                continue;
            }

            BlockPos skyCheckPosition =
                    sphereJar.getBlockPos().above();

            if (!level.canSeeSky(skyCheckPosition)) {
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
        }
    }

    private static void chargeExposedPlayerInventories(
            ServerLevel level,
            long pulseNumber
    ) {
        for (ServerPlayer player : level.players()) {
            if (!level.canSeeSky(player.blockPosition())) {
                continue;
            }

            var inventory = player.getInventory();
            var items = inventory.getNonEquipmentItems();
            int selectedSlot = inventory.getSelectedSlot();

            boolean changed = false;

            for (int slot = 0; slot < items.size(); slot++) {
                // Main-hand sphere already receives the faster held rate.
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

    private static void chargeExposedHeldSpheres(
            ServerLevel level,
            ServerPlayer player,
            long pulseNumber
    ) {
        if (!level.canSeeSky(player.blockPosition())) {
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

    private static void chargeExposedContainers(
            ServerLevel level,
            long pulseNumber
    ) {
        for (BlockEntity blockEntity : LOADED_CONTAINERS) {
            if (blockEntity.getLevel() != level
                    || blockEntity.isRemoved()
                    || blockEntity
                    instanceof SphereJarBlockEntity
                    || !(blockEntity
                    instanceof Container container)) {
                continue;
            }

            BlockPos skyCheckPosition =
                    blockEntity.getBlockPos().above();

            if (!level.canSeeSky(skyCheckPosition)) {
                continue;
            }

            boolean changed = false;

            for (int slot = 0;
                 slot < container.getContainerSize();
                 slot++) {

                ItemStack originalStack =
                        container.getItem(slot);

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
                    container.setItem(slot, updatedStack);
                    changed = true;
                }
            }

            if (changed) {
                container.setChanged();
            }
        }
    }

    private static void chargeExposedDroppedSpheres(
            ServerLevel level,
            long pulseNumber
    ) {
        for (ItemEntity itemEntity : level.getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                itemEntity ->
                        level.canSeeSky(
                                itemEntity.blockPosition()
                        )
        )) {
            ItemStack originalStack = itemEntity.getItem();

            if (!(originalStack.getItem()
                    instanceof SphereItem)) {
                continue;
            }

            ItemStack updatedStack = originalStack.copy();

            if (chargeSphere(updatedStack, pulseNumber)) {
                itemEntity.setItem(updatedStack);
            }
        }
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

        int capacity = sphere.getCapacity();
        int currentCharge = sphere.getCharge(stack);

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
                (elapsedTicks - 1)
                        * capacity
                        / fullChargeTicks;

        int chargeToAdd =
                (int) (currentProgress
                        - previousProgress);

        if (chargeToAdd <= 0) {
            return false;
        }

        sphere.setCharge(
                stack,
                Math.min(
                        capacity,
                        currentCharge + chargeToAdd
                )
        );

        return true;
    }

    private static boolean chargeSphere(
            ItemStack stack,
            long pulseNumber
    ) {
        if (!(stack.getItem() instanceof SphereItem sphere)) {
            return false;
        }

        int capacity = sphere.getCapacity();
        int currentCharge = sphere.getCharge(stack);

        if (currentCharge >= capacity) {
            return false;
        }

        long currentProgress =
                pulseNumber * capacity
                        / MAX_SPHERE_CAPACITY;

        long previousProgress =
                (pulseNumber - 1) * capacity
                        / MAX_SPHERE_CAPACITY;

        if (currentProgress <= previousProgress) {
            return false;
        }

        int chargeIncrease =
                (int) (currentProgress - previousProgress);

        sphere.setCharge(
                stack,
                currentCharge + chargeIncrease
        );
        return true;
    }

    private static void announce(
            MinecraftServer server,
            String message,
            ChatFormatting colour
    ) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(message).withStyle(colour),
                false
        );
    }
}
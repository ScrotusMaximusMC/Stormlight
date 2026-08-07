package com.scrotey.stormlight.highstorm;

import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;
import com.scrotey.stormlight.item.SphereItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HighstormManager {
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINECRAFT_DAY = 24_000;

    private static final int MIN_CALM_DAYS = 3;
    private static final int MAX_CALM_DAYS = 5;
    private static final int APPROACHING_TICKS = 60 * TICKS_PER_SECOND;
    private static final int HIGHSTORM_TICKS = 5 * 60 * TICKS_PER_SECOND;
    private static final int PASSING_TICKS = 30 * TICKS_PER_SECOND;
    private static final int WEATHER_BUFFER_TICKS = 5 * 60 * TICKS_PER_SECOND;

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

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> LOADED_CONTAINERS.clear()
        );

        ServerTickEvents.END_SERVER_TICK.register(
                HighstormManager::tick
        );
    }

    public static void startNow(MinecraftServer server) {
        ServerLevel level = server.overworld();
        HighstormSavedData data = HighstormSavedData.get(server);

        data.setPhase(
                HighstormPhase.HIGHSTORM,
                HIGHSTORM_TICKS
        );

        setHighstormWeather(level, HIGHSTORM_TICKS);

        announce(
                server,
                "A Highstorm has arrived!",
                ChatFormatting.AQUA
        );
    }

    public static void stopNow(MinecraftServer server) {
        ServerLevel level = server.overworld();
        HighstormSavedData data = HighstormSavedData.get(server);
        boolean wasInProgress =
                data.getPhase() != HighstormPhase.CALM;

        scheduleNextHighstorm(data, level.getRandom());
        releaseWeather(level);

        if (wasInProgress) {
            announce(
                    server,
                    "The Highstorm has passed.",
                    ChatFormatting.GRAY
            );
        }
    }

    public static boolean isHighstormActive(
            MinecraftServer server
    ) {
        HighstormSavedData data = HighstormSavedData.get(server);
        ensureInitialized(data, server.overworld().getRandom());
        return data.getPhase() == HighstormPhase.HIGHSTORM;
    }

    public static Component getStatus(MinecraftServer server) {
        HighstormSavedData data = HighstormSavedData.get(server);
        ensureInitialized(data, server.overworld().getRandom());

        return Component.literal("Highstorm phase: ")
                .append(
                        Component.literal(
                                data.getPhase().getDisplayName()
                        ).withStyle(colourFor(data.getPhase()))
                )
                .append(
                        Component.literal(
                                " — "
                                        + formatDuration(
                                                data.getTicksRemaining()
                                        )
                                        + " remaining"
                        )
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
        HighstormSavedData data = HighstormSavedData.get(server);

        ensureInitialized(data, level.getRandom());
        maintainWeather(level, data);

        if (data.getPhase() == HighstormPhase.HIGHSTORM) {
            long elapsedHighstormTicks =
                    HIGHSTORM_TICKS
                            - data.getTicksRemaining()
                            + 1;

            chargeDuringHighstorm(
                    level,
                    elapsedHighstormTicks
            );
        }

        data.tickDown();

        if (data.getTicksRemaining() == 0) {
            advancePhase(server, level, data);
        }
    }

    private static void ensureInitialized(
            HighstormSavedData data,
            RandomSource random
    ) {
        if (!data.isInitialized()) {
            scheduleNextHighstorm(data, random);
        }
    }

    private static void advancePhase(
            MinecraftServer server,
            ServerLevel level,
            HighstormSavedData data
    ) {
        switch (data.getPhase()) {
            case CALM -> {
                data.setPhase(
                        HighstormPhase.APPROACHING,
                        APPROACHING_TICKS
                );
                setRainWithoutThunder(level, APPROACHING_TICKS);
                announce(
                        server,
                        "A Highstorm is approaching!",
                        ChatFormatting.YELLOW
                );
            }
            case APPROACHING -> {
                data.setPhase(
                        HighstormPhase.HIGHSTORM,
                        HIGHSTORM_TICKS
                );
                setHighstormWeather(level, HIGHSTORM_TICKS);
                announce(
                        server,
                        "A Highstorm has arrived!",
                        ChatFormatting.AQUA
                );
            }
            case HIGHSTORM -> {
                data.setPhase(
                        HighstormPhase.PASSING,
                        PASSING_TICKS
                );
                setRainWithoutThunder(level, PASSING_TICKS);
                announce(
                        server,
                        "The Highstorm is passing.",
                        ChatFormatting.GRAY
                );
            }
            case PASSING -> {
                scheduleNextHighstorm(data, level.getRandom());
                releaseWeather(level);
                announce(
                        server,
                        "The Highstorm has passed.",
                        ChatFormatting.GRAY
                );
            }
        }
    }

    private static void scheduleNextHighstorm(
            HighstormSavedData data,
            RandomSource random
    ) {
        int calmDays = MIN_CALM_DAYS
                + random.nextInt(
                        MAX_CALM_DAYS - MIN_CALM_DAYS + 1
                );

        data.setPhase(
                HighstormPhase.CALM,
                (long) calmDays * TICKS_PER_MINECRAFT_DAY
        );
    }

    private static void maintainWeather(
            ServerLevel level,
            HighstormSavedData data
    ) {
        int remaining = (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(1, data.getTicksRemaining())
        );

        switch (data.getPhase()) {
            case CALM -> suppressVanillaThunder(level);
            case APPROACHING, PASSING -> {
                if (!level.isRaining() || level.isThundering()) {
                    setRainWithoutThunder(level, remaining);
                }
            }
            case HIGHSTORM -> {
                if (!level.isRaining() || !level.isThundering()) {
                    setHighstormWeather(level, remaining);
                }
            }
        }
    }

    private static void suppressVanillaThunder(ServerLevel level) {
        if (!level.isThundering()) {
            return;
        }

        if (level.isRaining()) {
            level.getServer().setWeatherParameters(
                    0,
                    WEATHER_BUFFER_TICKS,
                    true,
                    false
            );
        } else {
            level.getServer().setWeatherParameters(
                    WEATHER_BUFFER_TICKS,
                    0,
                    false,
                    false
            );
        }
    }

    private static void setRainWithoutThunder(
            ServerLevel level,
            int duration
    ) {
        level.getServer().setWeatherParameters(
                0,
                Math.max(1, duration),
                true,
                false
        );
    }

    private static void setHighstormWeather(
            ServerLevel level,
            int duration
    ) {
        level.getServer().setWeatherParameters(
                0,
                Math.max(1, duration),
                true,
                true
        );
    }

    private static void releaseWeather(ServerLevel level) {
        level.getServer().setWeatherParameters(
                WEATHER_BUFFER_TICKS,
                0,
                false,
                false
        );
    }

    private static void chargeDuringHighstorm(
            ServerLevel level,
            long elapsedHighstormTicks
    ) {
        chargeExposedSphereJars(
                level,
                elapsedHighstormTicks
        );

        if (elapsedHighstormTicks
                % HELD_AND_DROPPED_INTERVAL_TICKS == 0) {

            long pulseNumber =
                    elapsedHighstormTicks
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

        if (elapsedHighstormTicks
                % INVENTORY_INTERVAL_TICKS == 0) {

            long pulseNumber =
                    elapsedHighstormTicks
                            / INVENTORY_INTERVAL_TICKS;

            chargeExposedPlayerInventories(
                    level,
                    pulseNumber
            );
        }

        if (elapsedHighstormTicks
                % VANILLA_STORAGE_INTERVAL_TICKS == 0) {

            long pulseNumber =
                    elapsedHighstormTicks
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

    private static String formatDuration(long ticks) {
        if (ticks >= TICKS_PER_MINECRAFT_DAY) {
            return String.format(
                    Locale.ROOT,
                    "%.1f Minecraft days",
                    ticks / (double) TICKS_PER_MINECRAFT_DAY
            );
        }

        long totalSeconds =
                (ticks + TICKS_PER_SECOND - 1)
                        / TICKS_PER_SECOND;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format(
                Locale.ROOT,
                "%d:%02d",
                minutes,
                seconds
        );
    }

    private static ChatFormatting colourFor(
            HighstormPhase phase
    ) {
        return switch (phase) {
            case CALM -> ChatFormatting.GREEN;
            case APPROACHING -> ChatFormatting.YELLOW;
            case HIGHSTORM -> ChatFormatting.AQUA;
            case PASSING -> ChatFormatting.GRAY;
        };
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

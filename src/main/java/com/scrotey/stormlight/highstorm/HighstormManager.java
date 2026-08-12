package com.scrotey.stormlight.highstorm;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import com.scrotey.stormlight.network.HighstormVisualPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class HighstormManager {
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINECRAFT_DAY = 24_000;

    private static final int MIN_CALM_DAYS = 3;
    private static final int MAX_CALM_DAYS = 5;


    private static final int WEATHER_BUFFER_TICKS =
            5 * 60 * TICKS_PER_SECOND;

    private HighstormManager() {
    }

    public static void initialize() {
        HighstormCharging.initialize();

        ServerTickEvents.END_SERVER_TICK.register(
                HighstormManager::tick
        );
    }

    public static void startNow(
            MinecraftServer server
    ) {
        ServerLevel level =
                server.overworld();

        HighstormSavedData data =
                HighstormSavedData.get(server);

        data.setPhase(
                HighstormPhase.HIGHSTORM,
                HighstormTimings.HIGHSTORM_TICKS
        );

        setHighstormWeather(
                level,
                HighstormTimings.HIGHSTORM_TICKS
        );

        announce(
                server,
                "A Highstorm has arrived!",
                ChatFormatting.AQUA
        );
    }

    public static void startApproachNow(
            MinecraftServer server
    ) {
        ServerLevel level =
                server.overworld();

        HighstormSavedData data =
                HighstormSavedData.get(server);

        data.setPhase(
                HighstormPhase.APPROACHING,
                HighstormTimings.APPROACHING_TICKS
        );

        setRainWithoutThunder(
                level,
                HighstormTimings.APPROACHING_TICKS
        );

        announce(
                server,
                "A Highstorm is approaching!",
                ChatFormatting.YELLOW
        );
    }

    public static void startPassingNow(
            MinecraftServer server
    ) {
        ServerLevel level =
                server.overworld();

        HighstormSavedData data =
                HighstormSavedData.get(server);

        data.setPhase(
                HighstormPhase.PASSING,
                HighstormTimings.PASSING_TICKS
        );

        setRainWithoutThunder(
                level,
                HighstormTimings.PASSING_TICKS
        );

        announce(
                server,
                "The Highstorm is passing.",
                ChatFormatting.GRAY
        );
    }

    public static void stopNow(
            MinecraftServer server
    ) {
        ServerLevel level =
                server.overworld();

        HighstormSavedData data =
                HighstormSavedData.get(server);

        boolean wasInProgress =
                data.getPhase()
                        != HighstormPhase.CALM;

        scheduleNextHighstorm(
                data,
                level.getRandom()
        );

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
        HighstormSavedData data =
                HighstormSavedData.get(server);

        ensureInitialized(
                data,
                server.overworld().getRandom()
        );

        return data.getPhase()
                == HighstormPhase.HIGHSTORM;
    }

    public static Component getStatus(
            MinecraftServer server
    ) {
        HighstormSavedData data =
                HighstormSavedData.get(server);

        ensureInitialized(
                data,
                server.overworld().getRandom()
        );

        return Component.literal(
                "Highstorm phase: "
        ).append(
                Component.literal(
                        data.getPhase()
                                .getDisplayName()
                ).withStyle(
                        colourFor(data.getPhase())
                )
        ).append(
                Component.literal(
                        " — "
                                + formatDuration(
                                data.getTicksRemaining()
                        )
                                + " remaining"
                )
        );
    }

    private static void tick(
            MinecraftServer server
    ) {
        ServerLevel level =
                server.overworld();

        HighstormSavedData data =
                HighstormSavedData.get(server);

        ensureInitialized(
                data,
                level.getRandom()
        );

        maintainWeather(
                level,
                data
        );

        HighstormAtmosphere.tick(
                level,
                data.getPhase(),
                data.getTicksRemaining()
        );

        if (level.getGameTime() % 10L == 0L) {
            syncVisualState(
                    level,
                    data
            );
        }

        if (data.getPhase()
                == HighstormPhase.HIGHSTORM) {

            long elapsedHighstormTicks =
                    HighstormTimings.HIGHSTORM_TICKS
                            - data.getTicksRemaining()
                            + 1L;

            HighstormCharging.tick(
                    level,
                    elapsedHighstormTicks
            );
        }

        data.tickDown();

        if (data.getTicksRemaining() == 0L) {
            advancePhase(
                    server,
                    level,
                    data
            );
        }
    }

    private static void ensureInitialized(
            HighstormSavedData data,
            RandomSource random
    ) {
        if (!data.isInitialized()) {
            scheduleNextHighstorm(
                    data,
                    random
            );
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
                        HighstormTimings.APPROACHING_TICKS
                );

                setRainWithoutThunder(
                        level,
                        HighstormTimings.APPROACHING_TICKS
                );

                announce(
                        server,
                        "A Highstorm is approaching!",
                        ChatFormatting.YELLOW
                );
            }

            case APPROACHING -> {
                data.setPhase(
                        HighstormPhase.HIGHSTORM,
                        HighstormTimings.HIGHSTORM_TICKS
                );

                setHighstormWeather(
                        level,
                        HighstormTimings.HIGHSTORM_TICKS
                );

                announce(
                        server,
                        "A Highstorm has arrived!",
                        ChatFormatting.AQUA
                );
            }

            case HIGHSTORM -> {
                data.setPhase(
                        HighstormPhase.PASSING,
                        HighstormTimings.PASSING_TICKS
                );

                setRainWithoutThunder(
                        level,
                        HighstormTimings.PASSING_TICKS
                );

                announce(
                        server,
                        "The Highstorm is passing.",
                        ChatFormatting.GRAY
                );
            }

            case PASSING -> {
                scheduleNextHighstorm(
                        data,
                        level.getRandom()
                );

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
        int calmDays =
                MIN_CALM_DAYS
                        + random.nextInt(
                        MAX_CALM_DAYS
                                - MIN_CALM_DAYS
                                + 1
                );

        data.setPhase(
                HighstormPhase.CALM,
                (long) calmDays
                        * TICKS_PER_MINECRAFT_DAY
        );
    }

    private static void maintainWeather(
            ServerLevel level,
            HighstormSavedData data
    ) {
        int remaining =
                (int) Math.min(
                        Integer.MAX_VALUE,
                        Math.max(
                                1L,
                                data.getTicksRemaining()
                        )
                );

        switch (data.getPhase()) {
            case CALM ->
                    suppressVanillaThunder(level);

            case APPROACHING, PASSING -> {
                if (!level.isRaining()
                        || level.isThundering()) {

                    setRainWithoutThunder(
                            level,
                            remaining
                    );
                }
            }

            case HIGHSTORM -> {
                if (!level.isRaining()
                        || !level.isThundering()) {

                    setHighstormWeather(
                            level,
                            remaining
                    );
                }
            }
        }
    }

    private static void suppressVanillaThunder(
            ServerLevel level
    ) {
        if (!level.isThundering()) {
            return;
        }

        if (level.isRaining()) {
            level.getServer()
                    .setWeatherParameters(
                            0,
                            WEATHER_BUFFER_TICKS,
                            true,
                            false
                    );
        } else {
            level.getServer()
                    .setWeatherParameters(
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
        level.getServer()
                .setWeatherParameters(
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
        level.getServer()
                .setWeatherParameters(
                        0,
                        Math.max(1, duration),
                        true,
                        true
                );
    }

    private static void releaseWeather(
            ServerLevel level
    ) {
        level.getServer()
                .setWeatherParameters(
                        WEATHER_BUFFER_TICKS,
                        0,
                        false,
                        false
                );
    }

    private static String formatDuration(
            long ticks
    ) {
        if (ticks >= TICKS_PER_MINECRAFT_DAY) {
            return String.format(
                    Locale.ROOT,
                    "%.1f Minecraft days",
                    ticks
                            / (double)
                            TICKS_PER_MINECRAFT_DAY
            );
        }

        long totalSeconds =
                (ticks + TICKS_PER_SECOND - 1)
                        / TICKS_PER_SECOND;

        long minutes =
                totalSeconds / 60;

        long seconds =
                totalSeconds % 60;

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
            case CALM ->
                    ChatFormatting.GREEN;

            case APPROACHING ->
                    ChatFormatting.YELLOW;

            case HIGHSTORM ->
                    ChatFormatting.AQUA;

            case PASSING ->
                    ChatFormatting.GRAY;
        };
    }

    private static void syncVisualState(
            ServerLevel level,
            HighstormSavedData data
    ) {
        int ticksRemaining = (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(
                        0L,
                        data.getTicksRemaining()
                )
        );

        HighstormVisualPayload payload =
                new HighstormVisualPayload(
                        data.getPhase().ordinal(),
                        ticksRemaining
                );

        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(
                    player,
                    payload
            );
        }
    }

    private static void announce(
            MinecraftServer server,
            String message,
            ChatFormatting colour
    ) {
        server.getPlayerList()
                .broadcastSystemMessage(
                        Component.literal(message)
                                .withStyle(colour),
                        false
                );
    }
}

package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.network.ShardbladeStatePayload;
import com.scrotey.stormlight.progression.SprenBondProgression;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server authority for the summoned blade and its virtual held-item slot. */
public final class ShardbladeManager {
    private static final Map<UUID, BladeState> ACTIVE = new HashMap<>();
    private static final int DRAW_TICKS = 12;

    private ShardbladeManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ShardbladeManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            ACTIVE.remove(player.getUUID());
            ((BladeInventory) player.getInventory())
                    .stormlight$setBladeSelected(false);
        });
    }

    public static boolean isSummoned(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void handleInput(ServerPlayer player, int action) {
        if (action == 0) {
            if (!isSummoned(player)) {
                summon(player);
            }
        } else if (action == 2) {
            dismiss(player);
        }
    }

    private static void summon(ServerPlayer player) {
        if (SprenBondProgression.getLevel(player) < 3) {
            player.sendOverlayMessage(Component.translatable(
                    "message.stormlight.shardblade.locked"));
            return;
        }
        SprenEntity spren = SprenManager.getFor(player);
        if (spren == null) {
            player.sendOverlayMessage(Component.translatable(
                    "message.stormlight.spren_seek.no_spren"));
            return;
        }
        SprenSeekManager.cancel(player);
        SprenShieldManager.cancel(player);
        spren.startBlade();
        ACTIVE.put(player.getUUID(), new BladeState(spren.getUUID()));
        select(player);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS,
                0.9F, 1.5F);
    }

    public static void dismiss(ServerPlayer player) {
        BladeState removed = ACTIVE.remove(player.getUUID());
        if (removed == null) return;
        ((BladeInventory) player.getInventory())
                .stormlight$setBladeSelected(false);
        SprenEntity spren = SprenManager.getFor(player);
        if (spren != null && spren.getUUID().equals(removed.sprenUuid)) {
            spren.stopBlade();
        }
        sync(player);
    }

    private static void select(ServerPlayer player) {
        BladeState state = ACTIVE.get(player.getUUID());
        if (state == null || state.selected) return;
        state.selected = true;
        state.ready = false;
        state.drawAt = player.level().getGameTime() + DRAW_TICKS;
        BladeInventory inventory = (BladeInventory) player.getInventory();
        inventory.stormlight$setBladeSelected(true);
        inventory.stormlight$setBladeVisible(false);
        sync(player);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                1.0F, 1.65F);
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, BladeState>> iterator =
                ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(
                    entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            BladeState state = entry.getValue();
            SprenEntity spren = SprenManager.getFor(player);
            if (!player.isAlive()
                    || SprenBondProgression.getLevel(player) < 3
                    || spren == null
                    || !spren.getUUID().equals(state.sprenUuid)) {
                iterator.remove();
                ((BladeInventory) player.getInventory())
                        .stormlight$setBladeSelected(false);
                if (spren != null) spren.stopBlade();
                sync(player);
                continue;
            }
            if (state.selected && !state.ready
                    && player.level().getGameTime() >= state.drawAt
                    && spren.isBladeArrived()) {
                state.ready = true;
                ((BladeInventory) player.getInventory())
                        .stormlight$setBladeVisible(true);
                sync(player);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, 0.65F, 1.75F);
            }
        }
    }

    private static void sync(ServerPlayer player) {
        BladeState state = ACTIVE.get(player.getUUID());
        ServerPlayNetworking.send(player, state == null
                ? new ShardbladeStatePayload(false, false, false)
                : new ShardbladeStatePayload(true,
                        state.selected, state.ready));
    }

    private static final class BladeState {
        private final UUID sprenUuid;
        private boolean selected;
        private boolean ready;
        private long drawAt;

        private BladeState(UUID sprenUuid) {
            this.sprenUuid = sprenUuid;
        }
    }
}

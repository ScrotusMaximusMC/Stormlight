package com.scrotey.stormlight.client.lashing;

import com.scrotey.stormlight.client.StormlightHud;
import com.scrotey.stormlight.lashing.ExternalLashingManager;
import com.scrotey.stormlight.network.ExternalLashingTargetPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClientExternalLashingState {
    private static final int HOVER_COLOUR = 0x8DEBFF;
    private static final int SELECTED_COLOUR = 0xFFD166;

    private static int hoveredEntityId = ExternalLashingTargetPayload.NO_TARGET;
    private static int selectedEntityId = ExternalLashingTargetPayload.NO_TARGET;

    private ClientExternalLashingState() {
    }

    public static void update(ExternalLashingTargetPayload payload) {
        selectedEntityId = payload.entityId();

        if (selectedEntityId != ExternalLashingTargetPayload.NO_TARGET) {
            hoveredEntityId = ExternalLashingTargetPayload.NO_TARGET;
        }
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            clear();
            return;
        }

        if (selectedEntityId != ExternalLashingTargetPayload.NO_TARGET
                || !StormlightHud.canUseExternalLashing()) {
            hoveredEntityId = ExternalLashingTargetPayload.NO_TARGET;
            return;
        }

        LivingEntity target = findLookedAtTarget(client);
        hoveredEntityId = target == null
                ? ExternalLashingTargetPayload.NO_TARGET
                : target.getId();
    }

    public static boolean shouldOutline(Entity entity) {
        int entityId = entity.getId();
        return entityId == selectedEntityId
                || entityId == hoveredEntityId;
    }

    public static int outlineColour(Entity entity) {
        return entity.getId() == selectedEntityId
                ? SELECTED_COLOUR
                : HOVER_COLOUR;
    }

    private static LivingEntity findLookedAtTarget(Minecraft client) {
        Vec3 start = client.player.getEyePosition();
        Vec3 view = client.player.getLookAngle();
        Vec3 end = start.add(
                view.scale(ExternalLashingManager.TARGETING_RANGE)
        );
        HitResult blockHit = client.player.pick(
                ExternalLashingManager.TARGETING_RANGE,
                1.0F,
                false
        );

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB searchBox = client.player.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                client.player,
                start,
                end,
                searchBox,
                ClientExternalLashingState::isValidTarget,
                start.distanceToSqr(end)
        );

        return hit != null && hit.getEntity() instanceof LivingEntity living
                ? living
                : null;
    }

    private static boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity living
                && !(living instanceof Player)
                && living.isAlive()
                && living.isPickable();
    }

    private static void clear() {
        hoveredEntityId = ExternalLashingTargetPayload.NO_TARGET;
        selectedEntityId = ExternalLashingTargetPayload.NO_TARGET;
    }
}

package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.lashing.LashingManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player's bonded spren.
 *
 * The Allay inheritance is deliberately temporary: it gives us a pleasant
 * vanilla visual while all personality and movement are Stormlight-owned.
 * The spren does not run vanilla Allay AI.
 */
public final class SprenEntity extends Allay {
    private static final String OWNER_MOST_TAG = "StormlightOwnerMost";
    private static final String OWNER_LEAST_TAG = "StormlightOwnerLeast";

    private static final double MAX_WANDER_RADIUS = 50.0;
    private static final double HARD_RETURN_DISTANCE = 72.0;
    private static final double HARD_RETURN_DISTANCE_SQR =
            HARD_RETURN_DISTANCE * HARD_RETURN_DISTANCE;

    private static final double NORMAL_SPEED = 0.22;
    private static final double FAST_SPEED = 0.34;
    private static final double EXCITED_SPEED = 0.43;
    private static final double DART_SPEED = 0.52;
    private static final double ARRIVAL_DISTANCE = 0.65;

    private static final int MIN_DECISION_GAP = 30;
    private static final int STORMLIGHT_REACTION_COOLDOWN = 220;
    private static final int LOOK_REACTION_COOLDOWN = 120;
    private static final int INVESTIGATION_COOLDOWN = 180;
    private static final int DART_COOLDOWN = 240;

    private @Nullable UUID ownerUuid;
    private Activity activity = Activity.WANDER;
    private Vec3 target = Vec3.ZERO;
    private Vec3 visitOffset = Vec3.ZERO;
    private int activityTicks;
    private boolean targetChosen;

    private int decisionCooldown;
    private int stormlightReactionCooldown;
    private int investigationCooldown;
    private int dartCooldown;
    private int ownerIdleTicks;

    private boolean ownerHadStormlight;
    private boolean ownerWasLashing;
    private Vec3 lastOwnerPosition = Vec3.ZERO;
    private long activitySeed;

    private @Nullable UUID interestingEntityUuid;
    private @Nullable BlockPos interestingBlock;

    public SprenEntity(
            EntityType<? extends Allay> entityType,
            Level level
    ) {
        super(entityType, level);

        setNoGravity(true);
        setInvulnerable(true);
        setCanPickUpLoot(false);
        xpReward = 0;
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Allay.createAttributes();
    }

    public void setOwner(ServerPlayer player) {
        ownerUuid = player.getUUID();
        lastOwnerPosition = player.position();

    }

    public @Nullable UUID getOwnerUuid() {
        return ownerUuid;
    }

    public boolean isOwnedBy(Player player) {
        return ownerUuid != null
                && ownerUuid.equals(player.getUUID());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // No AllayAi: the Allay is only our placeholder visual shell.
    }

    @Override
    public void tick() {
        super.tick();

        setNoGravity(true);
        setInvulnerable(true);
        noPhysics = true;

        if (level().isClientSide()
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        tickSprenAi(serverLevel);
    }

    private void tickSprenAi(ServerLevel level) {
        ServerPlayer owner = getOwner(level);

        if (owner == null) {
            setDeltaMovement(getDeltaMovement().scale(0.75));
            return;
        }

        if (owner.level() != level()) {
            discard();
            return;
        }

        tickCooldowns();
        updateOwnerActivity(owner);

        if (distanceToSqr(owner) > HARD_RETURN_DISTANCE_SQR) {
            // Emergency returns should still respect the player's personal
            // space.  Reappear a little above and off to one side rather than
            // directly beside the camera.
            Vec3 returnPoint = owner.position().add(2.2, 2.35, 1.1);
            setPos(returnPoint.x, returnPoint.y, returnPoint.z);
            setDeltaMovement(Vec3.ZERO);
            chooseVisit(owner, 100);
            return;
        }

        boolean hasStormlight =
                ModAttachments.getPersonalStormlight(owner) > 0;
        boolean lashing = LashingManager.isLashing(owner)
                || LashingManager.isStabilising(owner);

        // Starting to glow or lash gets an immediate, visible reaction.
        if (lashing && !ownerWasLashing) {
            chooseExcitedOrbit(owner, 150);
        } else if (hasStormlight
                && !ownerHadStormlight
                && stormlightReactionCooldown <= 0) {
            chooseExcitedOrbit(owner, 100);
            stormlightReactionCooldown = STORMLIGHT_REACTION_COOLDOWN;
        }

        ownerHadStormlight = hasStormlight;
        ownerWasLashing = lashing;

        if (lashing) {
            maintainLashingCompanionship(owner);
        } else {
            if ((!targetChosen || activityTicks-- <= 0)
                    && decisionCooldown <= 0) {
                chooseNextActivity(owner, level, hasStormlight);
                decisionCooldown = MIN_DECISION_GAP;
            }
        }

        updateDynamicTarget(owner, level);
        moveTowardTarget(owner, hasStormlight || lashing);
    }

    private void tickCooldowns() {
        if (decisionCooldown > 0) decisionCooldown--;
        if (stormlightReactionCooldown > 0) stormlightReactionCooldown--;
        if (investigationCooldown > 0) investigationCooldown--;
        if (dartCooldown > 0) dartCooldown--;
    }

    private void updateOwnerActivity(ServerPlayer owner) {
        Vec3 now = owner.position();

        if (lastOwnerPosition == Vec3.ZERO) {
            lastOwnerPosition = now;
        }

        double moved = now.distanceToSqr(lastOwnerPosition);
        boolean active = moved > 0.0025
                || owner.getDeltaMovement().horizontalDistanceSqr() > 0.0025;

        if (active) {
            ownerIdleTicks = 0;
        } else {
            ownerIdleTicks++;
        }

        lastOwnerPosition = now;
    }

    private @Nullable ServerPlayer getOwner(ServerLevel level) {
        if (ownerUuid == null || level.getServer() == null) {
            return null;
        }

        return level.getServer()
                .getPlayerList()
                .getPlayer(ownerUuid);
    }

    private void chooseNextActivity(
            ServerPlayer owner,
            ServerLevel level,
            boolean hasStormlight
    ) {
        clearInterest();

        boolean travelling = owner.getDeltaMovement()
                .horizontalDistanceSqr() > 0.018;

        // Calm players encourage calm spren: after ~12 seconds of idleness,
        // perching and nearby curiosity become much more common.
        if (ownerIdleTicks > 240) {
            int calmRoll = random.nextInt(100);

            if (calmRoll < 42 && choosePerch(owner, level)) {
                return;
            }
            if (calmRoll < 68 && tryChooseInvestigation(owner, level)) {
                return;
            }
            if (calmRoll < 82) {
                chooseVisit(owner, 90 + random.nextInt(80));
                return;
            }

            chooseWander(owner, true);
            return;
        }

        if (travelling && dartCooldown <= 0 && random.nextInt(100) < 22) {
            chooseTravelDart(owner);
            dartCooldown = DART_COOLDOWN + random.nextInt(180);
            return;
        }

        if (hasStormlight && random.nextInt(100) < 28) {
            chooseExcitedOrbit(owner, 80 + random.nextInt(80));
            return;
        }

        int roll = random.nextInt(100);

        if (roll < 16) {
            chooseVisit(owner, 70 + random.nextInt(90));
            return;
        }

        if (roll < 34 && choosePerch(owner, level)) {
            return;
        }

        if (roll < 55
                && investigationCooldown <= 0
                && tryChooseInvestigation(owner, level)) {
            investigationCooldown = INVESTIGATION_COOLDOWN
                    + random.nextInt(180);
            return;
        }

        if (roll < 65 && dartCooldown <= 0) {
            choosePlayfulDart(owner);
            dartCooldown = DART_COOLDOWN + random.nextInt(180);
            return;
        }

        chooseWander(owner, false);
    }

    private void chooseWander(ServerPlayer owner, boolean calm) {
        activity = Activity.WANDER;
        activityTicks = calm
                ? 180 + random.nextInt(260)
                : 120 + random.nextInt(241);

        chooseNextWanderLeg(owner, calm);
        targetChosen = true;
        activitySeed = random.nextLong();
    }

    private void chooseNextWanderLeg(ServerPlayer owner, boolean calm) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double maxRadius = calm ? 24.0 : MAX_WANDER_RADIUS;

        /*
         * Most legs branch from the spren's current position, which produces
         * natural curves and zig-zags.  Some still pick a broader player-
         * centred destination so she continues to explore the whole area
         * rather than slowly drifting away forever.
         */
        boolean localLeg = random.nextDouble() < 0.72;

        if (localLeg) {
            double legLength = calm
                    ? 4.0 + random.nextDouble() * 9.0
                    : 6.0 + random.nextDouble() * 15.0;
            double yOffset = calm
                    ? -1.5 + random.nextDouble() * 3.5
                    : -3.0 + random.nextDouble() * 7.0;

            Vec3 candidate = position().add(
                    Math.cos(angle) * legLength,
                    yOffset,
                    Math.sin(angle) * legLength
            );

            Vec3 fromOwner = candidate.subtract(owner.position());
            double horizontalDistance = Math.sqrt(
                    fromOwner.x * fromOwner.x + fromOwner.z * fromOwner.z
            );

            if (horizontalDistance > maxRadius) {
                double scale = maxRadius / horizontalDistance;
                candidate = new Vec3(
                        owner.getX() + fromOwner.x * scale,
                        candidate.y,
                        owner.getZ() + fromOwner.z * scale
                );
            }

            target = candidate;
        } else {
            double radius = 6.0
                    + Math.pow(random.nextDouble(), 0.72)
                    * (maxRadius - 6.0);
            double yOffset = calm
                    ? random.nextDouble() * 5.0
                    : -1.0 + random.nextDouble() * 9.0;

            target = owner.position().add(
                    Math.cos(angle) * radius,
                    yOffset + 1.5,
                    Math.sin(angle) * radius
            );
        }

        targetChosen = true;
    }

    private void chooseVisit(ServerPlayer owner, int duration) {
        activity = Activity.VISIT;
        activityTicks = duration;
        activitySeed = random.nextLong();
        chooseVisitOffset(owner);
        target = owner.position().add(visitOffset);
        targetChosen = true;
    }

    private void chooseExcitedOrbit(ServerPlayer owner, int duration) {
        activity = Activity.ORBIT;
        activityTicks = duration;
        activitySeed = random.nextLong();
        updateOrbitTarget(owner);
        targetChosen = true;
    }

    private void chooseTravelDart(ServerPlayer owner) {
        activity = Activity.TRAVEL_DART;
        activityTicks = 70 + random.nextInt(60);
        activitySeed = random.nextLong();

        Vec3 forward = owner.getLookAngle();
        forward = new Vec3(forward.x, 0.0, forward.z);
        if (forward.lengthSqr() < 0.001) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }

        Vec3 side = new Vec3(-forward.z, 0.0, forward.x)
                .scale((random.nextBoolean() ? 1.0 : -1.0)
                        * (2.0 + random.nextDouble() * 4.0));

        target = owner.position()
                .add(forward.scale(10.0 + random.nextDouble() * 12.0))
                .add(side)
                .add(0.0, 2.0 + random.nextDouble() * 3.0, 0.0);
        targetChosen = true;
    }

    private void choosePlayfulDart(ServerPlayer owner) {
        activity = Activity.PLAYFUL_DART;
        activityTicks = 45 + random.nextInt(45);
        activitySeed = random.nextLong();

        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = 8.0 + random.nextDouble() * 10.0;
        target = owner.position().add(
                Math.cos(angle) * radius,
                4.0 + random.nextDouble() * 8.0,
                Math.sin(angle) * radius
        );
        targetChosen = true;
    }

    private void chooseVisitOffset(ServerPlayer owner) {
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        double side = (activitySeed & 1L) == 0L ? 1.0 : -1.0;

        /*
         * Choose the visit position ONCE.  The offset then stays fixed while
         * the player looks around, so turning the camera toward the spren does
         * not make her rotate away around the player.
         */
        boolean overheadVisit = (activitySeed & 4L) != 0L;

        if (overheadVisit) {
            double sideAmount = 1.20 * side;
            double forwardAmount = -0.20;

            double sideX = Math.cos(yaw) * sideAmount;
            double sideZ = Math.sin(yaw) * sideAmount;
            double forwardX = -Math.sin(yaw) * forwardAmount;
            double forwardZ = Math.cos(yaw) * forwardAmount;

            visitOffset = new Vec3(
                    sideX + forwardX,
                    3.15,
                    sideZ + forwardZ
            );
            return;
        }

        double sideAmount = 2.25;
        double forwardAmount = -0.25;

        double sideX = Math.cos(yaw) * sideAmount * side;
        double sideZ = Math.sin(yaw) * sideAmount * side;
        double forwardX = -Math.sin(yaw) * forwardAmount;
        double forwardZ = Math.cos(yaw) * forwardAmount;

        visitOffset = new Vec3(
                sideX + forwardX,
                1.95,
                sideZ + forwardZ
        );
    }

    private void updateVisitTarget(ServerPlayer owner) {
        target = owner.position().add(visitOffset);
    }

    private void updateOrbitTarget(ServerPlayer owner) {
        double direction = (activitySeed & 1L) == 0L ? 1.0 : -1.0;
        double angle = (tickCount * 0.085 * direction)
                + ((activitySeed & 1023L) / 1023.0) * Math.PI * 2.0;
        double radius = ownerWasLashing ? 2.2 : 1.65;
        double lift = 1.7 + Math.sin(tickCount * 0.16) * 0.55;

        target = owner.position().add(
                Math.cos(angle) * radius,
                lift,
                Math.sin(angle) * radius
        );
    }

    private void maintainLashingCompanionship(ServerPlayer owner) {
        if (activity != Activity.ORBIT && activity != Activity.TRAVEL_DART) {
            chooseExcitedOrbit(owner, 160);
        }

        if (activityTicks-- <= 0) {
            // During sustained flight, alternate between close spirals and
            // short bursts ahead so the spren accompanies rather than trails.
            if (random.nextBoolean()) {
                chooseTravelDart(owner);
            } else {
                chooseExcitedOrbit(owner, 120 + random.nextInt(80));
            }
        }
    }

    private boolean tryChooseInvestigation(
            ServerPlayer owner,
            ServerLevel level
    ) {
        if (random.nextBoolean() && chooseInterestingEntity(owner, level)) {
            return true;
        }

        if (chooseInterestingBlock(owner, level)) {
            return true;
        }

        return chooseInterestingEntity(owner, level);
    }

    private boolean chooseInterestingEntity(
            ServerPlayer owner,
            ServerLevel level
    ) {
        AABB area = owner.getBoundingBox().inflate(14.0, 7.0, 14.0);
        List<Entity> candidates = new ArrayList<>();

        candidates.addAll(level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                item -> item.isAlive()
        ));

        candidates.addAll(level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != owner
                        && entity != this
        ));

        if (candidates.isEmpty()) {
            return false;
        }

        Entity chosen = candidates.get(random.nextInt(candidates.size()));
        interestingEntityUuid = chosen.getUUID();
        interestingBlock = null;
        activity = Activity.INVESTIGATE_ENTITY;
        activityTicks = 80 + random.nextInt(100);
        activitySeed = random.nextLong();
        updateEntityInvestigationTarget(level);
        targetChosen = true;
        return true;
    }

    private boolean chooseInterestingBlock(
            ServerPlayer owner,
            ServerLevel level
    ) {
        BlockPos origin = owner.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();

        for (int attempt = 0; attempt < 48; attempt++) {
            BlockPos pos = origin.offset(
                    random.nextInt(25) - 12,
                    random.nextInt(11) - 5,
                    random.nextInt(25) - 12
            );
            BlockState state = level.getBlockState(pos);

            if (isInterestingBlock(state)) {
                candidates.add(pos.immutable());
            }
        }

        if (candidates.isEmpty()) {
            return false;
        }

        interestingBlock = candidates.get(random.nextInt(candidates.size()));
        interestingEntityUuid = null;
        activity = Activity.INVESTIGATE_BLOCK;
        activityTicks = 80 + random.nextInt(100);
        activitySeed = random.nextLong();
        updateBlockInvestigationTarget();
        targetChosen = true;
        return true;
    }

    private static boolean isInterestingBlock(BlockState state) {
        return state.is(Blocks.CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.is(Blocks.LECTERN)
                || state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.BREWING_STAND)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.POPPY)
                || state.is(Blocks.DANDELION)
                || state.is(Blocks.BLUE_ORCHID)
                || state.is(Blocks.ALLIUM)
                || state.is(Blocks.AZURE_BLUET)
                || state.is(Blocks.OXEYE_DAISY)
                || state.is(Blocks.CORNFLOWER)
                || state.is(Blocks.LILY_OF_THE_VALLEY)
                || state.is(Blocks.SUNFLOWER);
    }

    private void updateDynamicTarget(
            ServerPlayer owner,
            ServerLevel level
    ) {
        switch (activity) {
            case VISIT -> updateVisitTarget(owner);
            case ORBIT -> updateOrbitTarget(owner);
            case INVESTIGATE_ENTITY -> {
                if (!updateEntityInvestigationTarget(level)) {
                    chooseWander(owner, false);
                }
            }
            case INVESTIGATE_BLOCK -> {
                if (interestingBlock == null
                        || !isInterestingBlock(
                                level.getBlockState(interestingBlock))) {
                    chooseWander(owner, false);
                } else {
                    updateBlockInvestigationTarget();
                }
            }
            case PERCH -> {
                if (!isPerchStillValid(level)) {
                    chooseWander(owner, false);
                }
            }
            case WANDER -> {
                if (target.distanceToSqr(owner.position())
                        > MAX_WANDER_RADIUS * MAX_WANDER_RADIUS) {
                    chooseWander(owner, false);
                } else if (position().distanceToSqr(target)
                        < ARRIVAL_DISTANCE * ARRIVAL_DISTANCE * 2.25) {
                    // Free-flight should never turn into purposeless hovering.
                    // Reaching a roaming waypoint immediately creates another
                    // leg while preserving the current activity timer, so she
                    // keeps flowing through the area until it is actually time
                    // to choose a different behaviour.
                    chooseNextWanderLeg(owner, ownerIdleTicks > 120);
                }
            }
            case TRAVEL_DART, PLAYFUL_DART -> {
                if (position().distanceToSqr(target) < 1.4) {
                    chooseVisit(owner, 50 + random.nextInt(50));
                }
            }
        }
    }

    private boolean updateEntityInvestigationTarget(ServerLevel level) {
        if (interestingEntityUuid == null) {
            return false;
        }

        Entity entity = level.getEntity(interestingEntityUuid);
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        double angle = (tickCount * 0.045)
                + ((activitySeed & 1023L) / 1023.0) * Math.PI * 2.0;
        double radius = entity instanceof Monster
                ? 3.4
                : entity instanceof LivingEntity ? 1.6 : 0.8;
        double height = entity.getBbHeight() * 0.65 + 0.35;

        target = entity.position().add(
                Math.cos(angle) * radius,
                height + Math.sin(tickCount * 0.10) * 0.25,
                Math.sin(angle) * radius
        );
        return true;
    }

    private void updateBlockInvestigationTarget() {
        if (interestingBlock == null) {
            return;
        }

        double angle = (tickCount * 0.038)
                + ((activitySeed & 1023L) / 1023.0) * Math.PI * 2.0;
        target = Vec3.atCenterOf(interestingBlock).add(
                Math.cos(angle) * 0.8,
                1.0 + Math.sin(tickCount * 0.09) * 0.2,
                Math.sin(angle) * 0.8
        );
    }

    private boolean choosePerch(
            ServerPlayer owner,
            ServerLevel level
    ) {
        BlockPos ownerPos = owner.blockPosition();

        for (int attempt = 0; attempt < 24; attempt++) {
            int x = ownerPos.getX() + random.nextInt(25) - 12;
            int z = ownerPos.getZ() + random.nextInt(25) - 12;
            int startY = ownerPos.getY() + 7;
            int endY = ownerPos.getY() - 7;

            for (int y = startY; y >= endY; y--) {
                BlockPos surface = new BlockPos(x, y, z);
                BlockPos above = surface.above();
                BlockPos twoAbove = above.above();
                BlockState surfaceState = level.getBlockState(surface);

                if (surfaceState.isAir()
                        || !surfaceState.getFluidState().isEmpty()
                        || !level.getBlockState(above).isAir()
                        || !level.getBlockState(twoAbove).isAir()) {
                    continue;
                }

                activity = Activity.PERCH;
                activityTicks = ownerIdleTicks > 240
                        ? 180 + random.nextInt(260)
                        : 90 + random.nextInt(151);
                target = Vec3.atCenterOf(surface).add(0.0, 0.82, 0.0);
                targetChosen = true;
                activitySeed = random.nextLong();
                clearInterest();
                return true;
            }
        }

        return false;
    }

    private boolean isPerchStillValid(ServerLevel level) {
        BlockPos surface = BlockPos.containing(
                target.x,
                target.y - 0.82,
                target.z
        );

        return !level.getBlockState(surface).isAir()
                && level.getBlockState(surface)
                        .getFluidState()
                        .isEmpty();
    }

    private void moveTowardTarget(
            ServerPlayer owner,
            boolean excited
    ) {
        Vec3 current = position();
        Vec3 difference = target.subtract(current);
        double distance = difference.length();

        if (distance < ARRIVAL_DISTANCE) {
            double bob = activity == Activity.PERCH
                    ? 0.0
                    : Math.sin((tickCount + getId()) * 0.11) * 0.012;

            Vec3 holdingVelocity = getDeltaMovement()
                    .scale(activity == Activity.PERCH ? 0.25 : 0.66)
                    .add(0.0, bob, 0.0);

            setDeltaMovement(holdingVelocity);

            if (activity == Activity.PERCH) {
                facePoint(owner.getEyePosition());
            }
            return;
        }

        double speed;
        if (activity == Activity.PLAYFUL_DART
                || activity == Activity.TRAVEL_DART) {
            speed = DART_SPEED;
        } else if (excited || activity == Activity.ORBIT) {
            speed = EXCITED_SPEED;
        } else if (distance > 12.0
                || distanceToSqr(owner) > 40.0 * 40.0) {
            speed = FAST_SPEED;
        } else {
            speed = NORMAL_SPEED;
        }

        Vec3 desired = difference.normalize().scale(speed);
        Vec3 currentVelocity = getDeltaMovement();
        double smoothing = speed >= EXCITED_SPEED ? 0.36 : 0.28;
        Vec3 nextVelocity = currentVelocity
                .scale(1.0 - smoothing)
                .add(desired.scale(smoothing));

        setDeltaMovement(nextVelocity);

        double horizontal = nextVelocity.horizontalDistanceSqr();
        if (horizontal > 0.0001) {
            setYRot((float)(Mth.atan2(
                    nextVelocity.z,
                    nextVelocity.x
            ) * Mth.RAD_TO_DEG) - 90.0F);
        }
    }

    private void facePoint(Vec3 point) {
        Vec3 difference = point.subtract(position());
        if (difference.horizontalDistanceSqr() > 0.0001) {
            setYRot((float)(Mth.atan2(
                    difference.z,
                    difference.x
            ) * Mth.RAD_TO_DEG) - 90.0F);
        }
    }

    private void clearInterest() {
        interestingEntityUuid = null;
        interestingBlock = null;
    }

    @Override
    public boolean hurtServer(
            ServerLevel level,
            DamageSource source,
            float damage
    ) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Spren have no physical collision response with entities.
    }

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        // Future extension point for spren-specific interactions/abilities.
        return InteractionResult.PASS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        if (ownerUuid != null) {
            output.putLong(
                    OWNER_MOST_TAG,
                    ownerUuid.getMostSignificantBits()
            );
            output.putLong(
                    OWNER_LEAST_TAG,
                    ownerUuid.getLeastSignificantBits()
            );
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        long most = input.getLongOr(OWNER_MOST_TAG, 0L);
        long least = input.getLongOr(OWNER_LEAST_TAG, 0L);

        if (most != 0L || least != 0L) {
            ownerUuid = new UUID(most, least);
        } else {
            ownerUuid = null;
        }

        targetChosen = false;
        activityTicks = 0;
        decisionCooldown = 0;
        clearInterest();
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    private enum Activity {
        WANDER,
        VISIT,
        PERCH,
        INVESTIGATE_ENTITY,
        INVESTIGATE_BLOCK,
        PLAYFUL_DART,
        TRAVEL_DART,
        ORBIT
    }
}

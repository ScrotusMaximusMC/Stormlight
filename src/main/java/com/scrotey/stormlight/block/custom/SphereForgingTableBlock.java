package com.scrotey.stormlight.block.custom;

import com.mojang.serialization.MapCodec;
import com.scrotey.stormlight.block.entity.ModBlockEntities;
import com.scrotey.stormlight.block.entity.SphereForgingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

/**
 * A 3 wide, 2 deep, 2 tall glass forge. Parts are addressed in a local frame where x runs along
 * {@link #FACING}, z runs from the front to the back, and y is up; pixel coordinates below use the
 * same frame across the whole structure. Column x 0 is a flight of steps up to the rim of the
 * crucible, column x 1 is the brick hearth with the crucible on top, and column x 2 is the casting
 * bench under the tap. The core part (0, 0, 0) holds the block entity.
 */
public class SphereForgingTableBlock extends BaseEntityBlock {
    public static final MapCodec<SphereForgingTableBlock> CODEC = simpleCodec(SphereForgingTableBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int WIDTH = 3, HEIGHT = 2, DEPTH = 2;
    public static final int TAP_COLUMN = 2;
    public static final IntegerProperty PART_X = IntegerProperty.create("x", 0, WIDTH - 1);
    public static final IntegerProperty PART_Y = IntegerProperty.create("y", 0, HEIGHT - 1);
    public static final IntegerProperty PART_Z = IntegerProperty.create("z", 0, DEPTH - 1);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Inside of the crucible, where thrown glass is caught. */
    public static final float[] VAT_INTERIOR = {18.5F, 17.5F, 5.5F, 31.5F, 34, 26.5F};

    /** Collision boxes in pixels across the whole structure; clipped to each part below. */
    private static final double[][] BOXES = {
            // Steps
            {0, 0, 0, 16, 8, 6}, {0, 0, 6, 16, 16, 12}, {0, 0, 12, 16, 24, 18}, {0, 0, 18, 16, 32, 32},
            // Hearth and the open crucible: a floor and four walls so glass can drop in
            {16, 0, 2, 34, 16, 30}, {17, 16, 4, 33, 17.5, 28}, {17, 17.5, 4, 33, 32, 5.5}, {17, 17.5, 26.5, 33, 32, 28},
            {17, 17.5, 5.5, 18.5, 32, 26.5}, {31.5, 17.5, 5.5, 33, 32, 26.5},
            // Chimney, back wall, casting bench, mould and tap
            {34, 0, 26, 41, 32, 32}, {41, 0, 30, 48, 20, 32}, {34, 0, 8, 47, 5, 24},
            {35.5, 5, 12.5, 42.5, 6.75, 19.5}, {33, 15.5, 15, 40, 19.5, 17}, {34, 19.5, 14, 38, 21.8, 18}};
    private static final Map<Direction, VoxelShape>[] SHAPES = buildShapes();

    public SphereForgingTableBlock(BlockBehaviour.Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(PART_X, 0)
                .setValue(PART_Y, 0).setValue(PART_Z, 0).setValue(LIT, false));
    }

    @SuppressWarnings("unchecked")
    private static Map<Direction, VoxelShape>[] buildShapes() {
        Map<Direction, VoxelShape>[] shapes = new Map[WIDTH * HEIGHT * DEPTH];
        for (int x = 0; x < WIDTH; x++) for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : BOXES) {
                double x0 = Math.max(b[0] - x * 16, 0), y0 = Math.max(b[1] - y * 16, 0), z0 = Math.max(b[2] - z * 16, 0);
                double x1 = Math.min(b[3] - x * 16, 16), y1 = Math.min(b[4] - y * 16, 16), z1 = Math.min(b[5] - z * 16, 16);
                if (x0 < x1 && y0 < y1 && z0 < z1) shape = Shapes.or(shape, Block.box(x0, y0, z0, x1, y1, z1));
            }
            shapes[index(x, y, z)] = Shapes.rotateHorizontal(shape);
        }
        return shapes;
    }

    private static int index(int x, int y, int z) { return (x * HEIGHT + y) * DEPTH + z; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static boolean isCore(BlockState state) {
        return state.getValue(PART_X) == 0 && state.getValue(PART_Y) == 0 && state.getValue(PART_Z) == 0;
    }

    /** World position of local part (x, y, z) for a forge whose core is at {@code core}. */
    public static BlockPos partPos(BlockPos core, Direction facing, int x, int y, int z) {
        return core.relative(facing, x).relative(facing.getClockWise(), z).above(y);
    }

    public static BlockPos corePos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return pos.relative(facing, -state.getValue(PART_X)).relative(facing.getClockWise(), -state.getValue(PART_Z))
                .below(state.getValue(PART_Y));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite().getClockWise());
        Level level = context.getLevel();
        BlockPos core = context.getClickedPos();
        for (int x = 0; x < WIDTH; x++) for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) {
            BlockPos part = partPos(core, state.getValue(FACING), x, y, z);
            if (part.equals(core)) continue;
            if (!level.getWorldBorder().isWithinBounds(part) || level.isOutsideBuildHeight(part)
                    || !level.getBlockState(part).canBeReplaced(context)) return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction facing = state.getValue(FACING);
        for (int x = 0; x < WIDTH; x++) for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            level.setBlock(partPos(pos, facing, x, y, z), state.setValue(PART_X, x).setValue(PART_Y, y)
                    .setValue(PART_Z, z).setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    /** Sets the lit flag on every part so the firebox model, light and particles change together. */
    public static void setLit(Level level, BlockPos core, BlockState coreState, boolean lit) {
        Direction facing = coreState.getValue(FACING);
        for (int x = 0; x < WIDTH; x++) for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) {
            BlockPos part = partPos(core, facing, x, y, z);
            BlockState state = level.getBlockState(part);
            if (state.getBlock() instanceof SphereForgingTableBlock && state.getValue(LIT) != lit) {
                level.setBlock(part, state.setValue(LIT, lit), Block.UPDATE_ALL);
            }
        }
    }

    /** World-space box covering the inside of the crucible. */
    public static AABB vatInterior(BlockPos core, Direction facing) {
        float[] v = VAT_INTERIOR;
        return new AABB(local(core, facing, v[0], v[1], v[2]), local(core, facing, v[3], v[4], v[5]));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighbor, RandomSource random) {
        Direction facing = state.getValue(FACING);
        int dx = 0, dy = 0, dz = 0;
        if (direction == facing) dx = 1;
        else if (direction == facing.getOpposite()) dx = -1;
        else if (direction == facing.getClockWise()) dz = 1;
        else if (direction == facing.getCounterClockWise()) dz = -1;
        else dy = direction == Direction.UP ? 1 : -1;
        int x = state.getValue(PART_X) + dx, y = state.getValue(PART_Y) + dy, z = state.getValue(PART_Z) + dz;
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && z >= 0 && z < DEPTH) {
            // Losing any neighbouring part takes the whole forge down with it.
            return neighbor.is(this) && neighbor.getValue(FACING) == facing && neighbor.getValue(PART_X) == x
                    && neighbor.getValue(PART_Y) == y && neighbor.getValue(PART_Z) == z
                    ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Survival drops the core via neighbour updates. Creative suppresses that drop.
        if (!level.isClientSide() && player.isCreative() && !isCore(state)) {
            BlockPos core = corePos(pos, state);
            BlockState other = level.getBlockState(core);
            if (other.is(this) && isCore(other) && other.getValue(FACING) == state.getValue(FACING)) {
                level.setBlock(core, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return use(state, level, pos, player, hand);
    }

    private InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.isClientSide()
                && level.getBlockEntity(corePos(pos, state)) instanceof SphereForgingTableBlockEntity forge) {
            forge.interact(player, hand, state.getValue(PART_X) == TAP_COLUMN);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isCore(state) || !state.getValue(LIT)) return;
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < 2; i++) {
            Vec3 p = local(pos, facing, 20 + random.nextFloat() * 10, 2 + random.nextFloat() * 5, 1.8F);
            level.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0, 0.01, 0);
        }
        if (random.nextInt(2) == 0) {
            Vec3 flue = local(pos, facing, 37.5F, 40.5F, 29);
            level.addParticle(ParticleTypes.LARGE_SMOKE, flue.x, flue.y, flue.z, 0, 0.06, 0);
        }
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.9F, 0.9F, false);
        }
    }

    /** Converts a pixel position in the forge's local frame to world space. */
    public static Vec3 local(BlockPos core, Direction facing, float x, float y, float z) {
        double dx = x / 16 - 0.5, dz = z / 16 - 0.5;
        double rx, rz;
        switch (facing) {
            case SOUTH -> { rx = -dz; rz = dx; }
            case WEST -> { rx = -dx; rz = -dz; }
            case NORTH -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        return new Vec3(core.getX() + 0.5 + rx, core.getY() + y / 16, core.getZ() + 0.5 + rz);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isCore(state) ? new SphereForgingTableBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return isCore(state)
                ? createTickerHelper(type, ModBlockEntities.SPHERE_FORGING_TABLE, SphereForgingTableBlockEntity::tick)
                : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[index(state.getValue(PART_X), state.getValue(PART_Y), state.getValue(PART_Z))]
                .get(state.getValue(FACING).getCounterClockWise());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_X, PART_Y, PART_Z, LIT);
    }
}

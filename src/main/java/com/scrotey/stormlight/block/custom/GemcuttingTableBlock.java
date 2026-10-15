package com.scrotey.stormlight.block.custom;

import com.mojang.serialization.MapCodec;
import com.scrotey.stormlight.block.entity.GemcuttingTableBlockEntity;
import com.scrotey.stormlight.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GemcuttingTableBlock extends BaseEntityBlock {
    public static final MapCodec<GemcuttingTableBlock> CODEC = simpleCodec(GemcuttingTableBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    private static VoxelShape frame(boolean right) {
        int x = right ? 12 : 1;
        return Shapes.or(Block.box(0, 11, 0, 16, 15, 16),
                Block.box(x, 0, 1, x + 3, 11, 4), Block.box(x, 0, 12, x + 3, 11, 15),
                Block.box(right ? 0 : 1, 3, 2, right ? 15 : 16, 4, 14));
    }
    private static final java.util.Map<Direction, VoxelShape> LEFT_SHAPES = Shapes.rotateHorizontal(
            Shapes.or(frame(false), Block.box(3, 15, 3, 16, 20, 12)));
    private static final java.util.Map<Direction, VoxelShape> RIGHT_SHAPES = Shapes.rotateHorizontal(
            Shapes.or(frame(true), Block.box(0, 15, 4, 5, 22, 11), Block.box(7, 15, 2, 14, 18, 13)));
    public GemcuttingTableBlock(BlockBehaviour.Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static Direction connectedDirection(BlockState state) {
        // Facing points along the bench, so mirroring keeps the inventory on its original half.
        Direction right = state.getValue(FACING);
        return state.getValue(RIGHT) ? right.getOpposite() : right;
    }

    public static BlockPos inventoryPos(BlockPos pos, BlockState state) {
        return state.getValue(RIGHT) ? pos.relative(connectedDirection(state)) : pos;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite().getClockWise());
        BlockPos other = context.getClickedPos().relative(connectedDirection(state));
        return context.getLevel().getWorldBorder().isWithinBounds(other)
                && context.getLevel().getBlockState(other).canBeReplaced(context) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.relative(connectedDirection(state)), state.setValue(RIGHT, true), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighbor, RandomSource random) {
        if (direction == connectedDirection(state)) {
            return neighbor.is(this) && neighbor.getValue(RIGHT) != state.getValue(RIGHT)
                    && neighbor.getValue(FACING) == state.getValue(FACING)
                    ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Survival drops the primary half via neighbor updates. Creative suppresses that drop.
        if (!level.isClientSide() && player.isCreative() && state.getValue(RIGHT)) {
            BlockPos primary = inventoryPos(pos, state);
            BlockState other = level.getBlockState(primary);
            if (other.is(this) && !other.getValue(RIGHT) && other.getValue(FACING) == state.getValue(FACING)) {
                level.setBlock(primary, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(inventoryPos(pos, state)) instanceof GemcuttingTableBlockEntity table) {
            table.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(inventoryPos(pos, state)) instanceof GemcuttingTableBlockEntity table) {
            table.interact(player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(RIGHT) ? null : new GemcuttingTableBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(RIGHT) ? null
                : createTickerHelper(type, ModBlockEntities.GEMCUTTING_TABLE, GemcuttingTableBlockEntity::tick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(RIGHT) ? RIGHT_SHAPES : LEFT_SHAPES)
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
        builder.add(FACING, RIGHT);
    }
}

package com.scrotey.stormlight.block.custom;

import com.mojang.serialization.MapCodec;
import com.scrotey.stormlight.block.entity.SphereLanternBlockEntity;
import com.scrotey.stormlight.item.SphereItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class SphereLanternBlock extends BaseEntityBlock {

    public static final BooleanProperty HANGING =
            BlockStateProperties.HANGING;

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final IntegerProperty LIGHT_LEVEL =
            IntegerProperty.create(
                    "light_level",
                    0,
                    15
            );

    /*
     * 0 = no sphere
     * 1 = diamond
     * 2 = garnet
     * 3 = ruby
     * 4 = sapphire
     * 5 = emerald
     */
    public static final IntegerProperty GEMSTONE =
            IntegerProperty.create(
                    "gemstone",
                    0,
                    5
            );

    private static final VoxelShape SHAPE =
            Block.box(
                    3.0,
                    1.0,
                    3.0,
                    13.0,
                    16.0,
                    13.0
            );

    public SphereLanternBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(HANGING, true)
                        .setValue(FACING, Direction.SOUTH)
                        .setValue(LIGHT_LEVEL, 0)
                        .setValue(GEMSTONE, 0)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SphereLanternBlock::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction clickedFace =
                context.getClickedFace();

        BlockState state;

        /*
         * Clicking the underside of a block creates a ceiling lantern.
         */
        if (clickedFace == Direction.DOWN) {
            state = defaultBlockState()
                    .setValue(HANGING, true)
                    .setValue(
                            FACING,
                            context.getHorizontalDirection()
                                    .getOpposite()
                    );
        }

        /*
         * Clicking a wall creates the bracket-mounted lantern.
         */
        else if (clickedFace.getAxis().isHorizontal()) {
            state = defaultBlockState()
                    .setValue(HANGING, false)
                    .setValue(FACING, clickedFace);
        }

        /*
         * The lantern cannot stand on the floor.
         */
        else {
            return null;
        }

        return state.canSurvive(
                context.getLevel(),
                context.getClickedPos()
        )
                ? state
                : null;
    }

    @Override
    protected boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        if (state.getValue(HANGING)) {
            BlockPos supportPos =
                    pos.above();

            return level.getBlockState(supportPos)
                    .isFaceSturdy(
                            level,
                            supportPos,
                            Direction.DOWN
                    );
        }

        Direction facing =
                state.getValue(FACING);

        BlockPos supportPos =
                pos.relative(facing.getOpposite());

        return level.getBlockState(supportPos)
                .isFaceSturdy(
                        level,
                        supportPos,
                        facing
                );
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTicks,
            BlockPos pos,
            Direction changedDirection,
            BlockPos changedPos,
            BlockState changedState,
            RandomSource random
    ) {
        Direction supportDirection =
                state.getValue(HANGING)
                        ? Direction.UP
                        : state.getValue(FACING)
                        .getOpposite();

        if (changedDirection == supportDirection
                && !state.canSurvive(level, pos)) {

            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(
                state,
                level,
                scheduledTicks,
                pos,
                changedDirection,
                changedPos,
                changedState,
                random
        );
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack heldStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(heldStack.getItem() instanceof SphereItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        /*
         * The client does not need the complete stored ItemStack.
         * The server validates and performs the insertion.
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos)
                instanceof SphereLanternBlockEntity lantern)) {

            return InteractionResult.PASS;
        }

        if (!lantern.insertSphere(heldStack)) {
            return InteractionResult.CONSUME;
        }

        if (!player.isCreative()) {
            heldStack.shrink(1);
        }

        level.playSound(
                null,
                pos,
                SoundEvents.LANTERN_PLACE,
                SoundSource.BLOCKS,
                0.8F,
                1.15F
        );

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos)
                instanceof SphereLanternBlockEntity lantern)) {

            return InteractionResult.PASS;
        }

        ItemStack removedSphere =
                lantern.removeSphere();

        if (removedSphere.isEmpty()) {
            return InteractionResult.PASS;
        }

        player.getInventory()
                .placeItemBackInInventory(
                        removedSphere
                );

        level.playSound(
                null,
                pos,
                SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                SoundSource.BLOCKS,
                0.8F,
                1.2F
        );

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {
        return state.setValue(
                FACING,
                rotation.rotate(
                        state.getValue(FACING)
                )
        );
    }

    @Override
    protected BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {
        return rotate(
                state,
                mirror.getRotation(
                        state.getValue(FACING)
                )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                HANGING,
                FACING,
                LIGHT_LEVEL,
                GEMSTONE
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SphereLanternBlockEntity(
                pos,
                state
        );
    }
}
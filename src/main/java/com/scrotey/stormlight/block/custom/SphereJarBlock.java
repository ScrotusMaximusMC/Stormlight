package com.scrotey.stormlight.block.custom;

import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SphereJarBlock extends BaseEntityBlock {
    public SphereJarBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(FILL_LEVEL, 0)
                        .setValue(LIGHT_LEVEL, 0)
        );

    }

    public static final IntegerProperty FILL_LEVEL =
            IntegerProperty.create("fill_level", 0, 4);

    public static final IntegerProperty LIGHT_LEVEL =
            IntegerProperty.create("light_level", 0, 15);


    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FILL_LEVEL, LIGHT_LEVEL);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SphereJarBlock::new);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos)
                instanceof SphereJarBlockEntity sphereJar) {

            player.openMenu(sphereJar);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SphereJarBlockEntity(
                pos,
                state
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }
}
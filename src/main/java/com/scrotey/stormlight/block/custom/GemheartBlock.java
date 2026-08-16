package com.scrotey.stormlight.block.custom;

import com.scrotey.stormlight.worldgen.chrysalis.ChrysalisSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GemheartBlock extends Block {

    /*
     * A long diagonal gemheart with a fuller middle and a slightly more
     * pronounced underside bulge.
     */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0, 13.8, 1.0, 2.5, 16.0, 2.5),
            Block.box(1.6, 12.0, 1.8, 4.2, 14.4, 4.4),
            Block.box(2.4, 10.3, 2.8, 6.3, 13.0, 6.7),
            Block.box(3.4, 8.8, 3.9, 8.5, 11.8, 9.0),
            Block.box(4.3, 7.1, 4.9, 10.8, 10.3, 11.3),

            // Slightly fuller lower side of the central bulge
            Block.box(4.0, 5.9, 4.8, 11.2, 8.4, 11.8),
            Block.box(5.0, 4.8, 5.8, 11.8, 7.0, 12.6),

            Block.box(7.0, 3.8, 8.0, 12.0, 6.7, 13.0),
            Block.box(9.2, 2.2, 10.2, 13.2, 4.8, 14.2),
            Block.box(11.6, 0.8, 12.6, 14.4, 2.8, 15.2),
            Block.box(13.3, 0.0, 13.3, 15.0, 1.4, 15.0)
    );

    public GemheartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ChrysalisSavedData.get(serverLevel.getServer())
                    .markGemheartMined(pos);
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
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
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }
}
package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SprenSeekOres {
    private SprenSeekOres() {
    }

    public static boolean isOre(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE)
                || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(ModBlocks.RUBY_ORE)
                || state.is(ModBlocks.DEEPSLATE_RUBY_ORE)
                || state.is(ModBlocks.GARNET_ORE)
                || state.is(ModBlocks.DEEPSLATE_GARNET_ORE)
                || state.is(ModBlocks.SAPPHIRE_ORE)
                || state.is(ModBlocks.DEEPSLATE_SAPPHIRE_ORE);
    }
}
